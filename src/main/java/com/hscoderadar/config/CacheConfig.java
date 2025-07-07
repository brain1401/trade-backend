package com.hscoderadar.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

  @Bean
  public CacheManager cacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager("translations");
    cacheManager.setAsyncCacheMode(true);
    cacheManager.setCaffeine(Caffeine.newBuilder()
        // 캐시 항목이 작성된 후 24시간이 지나면 자동으로 제거
        .expireAfterWrite(24, TimeUnit.HOURS)
        // 캐시에 최대 10,000개의 번역 결과를 저장
        .maximumSize(10_000));
    return cacheManager;
  }
}