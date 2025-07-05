package com.hscoderadar.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import java.time.Duration;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;
import java.util.concurrent.TimeUnit;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

/**
 * WebClient 설정 클래스
 * SSL/HTTP 연결 안정성 및 타임아웃 처리를 개선
 */
@Configuration
public class WebClientConfig {

  @Bean
  public WebClient webClient(ObjectMapper objectMapper) {
    XmlMapper xmlMapper = new XmlMapper();
    xmlMapper.registerModule(new JakartaXmlBindAnnotationModule());
    xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    ExchangeStrategies strategies = ExchangeStrategies.builder()
        .codecs(configurer -> {
          configurer.defaultCodecs()
              .jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON));

          // 커스텀 디코더 등록
          configurer.customCodecs().register(new Jackson2JsonDecoder(xmlMapper, new MediaType("application", "xml")));
          configurer.customCodecs().register(new Jackson2JsonDecoder(xmlMapper, new MediaType("text", "xml")));

        })
        .build();

    return WebClient.builder()
        .baseUrl("https://unipass.customs.go.kr:38010")
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE)
        .exchangeStrategies(strategies)
        .build();
  }

  @Bean
  public WebClient comtradeWebClient() {
    ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(50 * 1024 * 1024))
        .build();

    return WebClient.builder()
        .baseUrl("https://comtradeapi.un.org")
        .exchangeStrategies(exchangeStrategies)
        .build();
  }

  /**
   * Python AI 서버 통신용 WebClient (SSL/HTTP 안정성 개선)
   */
  @Bean
  public WebClient pythonAiWebClient(@Value("${ai.python.server.url}") String baseUrl,
      @Value("${ai.python.server.timeout.connect:10000}") int connectTimeout,
      @Value("${ai.python.server.timeout.read:30000}") int readTimeout) {

    // SSE 스트리밍을 위한 대용량 버퍼 설정
    ExchangeStrategies strategies = ExchangeStrategies.builder()
        .codecs(configurer -> {
          configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024); // 16MB
        })
        .build();

    // HTTP/HTTPS 안정성을 위한 HttpClient 설정 (EOFException 방지 개선)
    HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
        .option(ChannelOption.SO_KEEPALIVE, true)
        .option(ChannelOption.TCP_NODELAY, true)
        .responseTimeout(Duration.ofMillis(readTimeout))
        .doOnConnected(conn -> conn
            .addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
            .addHandlerLast(new WriteTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS)))
        // SSL 관련 에러 방지를 위한 설정
        .secure(sslSpec -> {
          try {
            sslSpec.sslContext(SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE) // 개발용: 자체 서명 인증서 허용
                .build());
          } catch (Exception e) {
            throw new RuntimeException("SSL context 설정 실패", e);
          }
        })
        // Keep-alive 설정으로 연결 안정성 향상
        .keepAlive(true)
        // 연결 오류 시 재시도 메커니즘
        .wiretap(true); // 네트워크 디버깅용

    return WebClient.builder()
        .baseUrl(baseUrl)
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .exchangeStrategies(strategies)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
        .defaultHeader(HttpHeaders.CONNECTION, "keep-alive")
        .defaultHeader(HttpHeaders.USER_AGENT, "HSCodeRadar/6.1")
        .build();
  }
}