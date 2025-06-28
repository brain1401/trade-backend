package com.hscoderadar.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(
    basePackages = {
      "com.hscoderadar.domain.user.repository",
      "com.hscoderadar.domain.chat.repository",
      "com.hscoderadar.domain.news.repository",
      "com.hscoderadar.domain.exchange.repository"
    },
    repositoryImplementationPostfix = "Impl")
@EnableJpaAuditing
public class JpaConfig {

  // JPA 관련 추가 설정이 필요한 경우 여기에 Bean 정의
}
