package com.hscoderadar.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * v4.1 Redis Configuration (Enhanced)
 * 
 * ChatGPT 스타일 통합 채팅의 일회용 토큰 시스템 + SMS 인증 세션 관리
 * - 세션 토큰 관리 (생성 → 10분 유효 → 일회 사용 후 즉시 삭제)
 * - SMS 인증 세션 관리 (5분 TTL 자동 만료)
 * - 쿨다운 및 시도 횟수 제한 관리
 * - 높은 성능의 토큰 검증 (1ms 이내)
 */
@Configuration
@EnableRedisRepositories
public class RedisConfig {

  @Value("${spring.data.redis.host:localhost}")
  private String redisHost;

  @Value("${spring.data.redis.port:6379}")
  private int redisPort;

  @Value("${spring.data.redis.password:}")
  private String redisPassword;

  /**
   * Redis 연결 팩토리 설정
   */
  @Bean
  public RedisConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
    config.setHostName(redisHost);
    config.setPort(redisPort);

    // 비밀번호가 설정된 경우에만 적용
    if (redisPassword != null && !redisPassword.trim().isEmpty()) {
      config.setPassword(redisPassword);
    }

    return new LettuceConnectionFactory(config);
  }

  /**
   * Redis Template 설정 (문자열 기반 토큰 관리용)
   */
  @Bean
  public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, String> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    // 키와 값 모두 문자열로 직렬화 (토큰 시스템에 최적화)
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(new StringRedisSerializer());

    return template;
  }

  /**
   * Redis Template 설정 (객체 저장용)
   */
  @Bean
  public RedisTemplate<String, Object> redisObjectTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    // 키는 문자열, 값은 JSON으로 직렬화
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

    return template;
  }
}