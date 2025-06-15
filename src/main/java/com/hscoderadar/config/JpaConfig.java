package com.hscoderadar.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = {
    "com.hscoderadar.domain.user.repository",
    "com.hscoderadar.domain.bookmark.repository",
    "com.hscoderadar.domain.hscode.repository",
    "com.hscoderadar.domain.monitoring.repository",
    "com.hscoderadar.domain.notification.repository"
})
@EnableTransactionManagement
public class JpaConfig {

  // JPA 관련 추가 설정이 필요한 경우 여기에 Bean 정의
}