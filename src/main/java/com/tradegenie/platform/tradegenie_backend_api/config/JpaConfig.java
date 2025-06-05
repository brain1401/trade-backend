package com.tradegenie.platform.tradegenie_backend_api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = "com.tradegenie.platform.tradegenie_backend_api.repository")
@EnableTransactionManagement
public class JpaConfig {

  // JPA 관련 추가 설정이 필요한 경우 여기에 Bean 정의
}