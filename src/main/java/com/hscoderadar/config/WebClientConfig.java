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
                .codecs(configurer ->
                        configurer.defaultCodecs().maxInMemorySize(50 * 1024 * 1024))
                .build();

        
        return WebClient.builder()
                .baseUrl("https://comtradeapi.un.org")
                .exchangeStrategies(exchangeStrategies) 
                .build();
  }
}