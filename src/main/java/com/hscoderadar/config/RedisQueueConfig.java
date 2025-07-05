package com.hscoderadar.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Redis Queue 설정
 * 알림 큐 처리를 위한 RedisTemplate 및 TaskExecutor 구성
 */
@Configuration
public class RedisQueueConfig {

  /**
   * Redis Queue용 RedisTemplate 설정
   * String 키와 JSON 값 직렬화 사용
   */
  @Bean
  public RedisTemplate<String, Object> queueRedisTemplate(
      RedisConnectionFactory connectionFactory,
      ObjectMapper objectMapper) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    // 키 직렬화: String
    template.setKeySerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());

    // 값 직렬화: JSON
    GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
    template.setValueSerializer(jsonSerializer);
    template.setHashValueSerializer(jsonSerializer);

    template.afterPropertiesSet();
    return template;
  }

  /**
   * 알림 소비자용 TaskExecutor
   * 백그라운드 스레드에서 Redis Queue를 모니터링
   */
  @Bean(name = "notificationTaskExecutor")
  public Executor notificationTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("notification-consumer-");
    executor.setRejectedExecutionHandler((r, exec) -> {
      // 큐가 가득 찰 경우 처리
      throw new RuntimeException("알림 처리 큐가 가득 참");
    });
    executor.initialize();
    return executor;
  }
}