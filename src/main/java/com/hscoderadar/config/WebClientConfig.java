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

          // X커스텀 디코더 등록
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

    // Netty HttpClient 타임아웃 설정
    HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
        .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
            .addHandlerLast(new WriteTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS)));

    return WebClient.builder()
        .baseUrl(baseUrl)
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .exchangeStrategies(strategies)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
        .build();
  }
}