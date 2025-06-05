package com.tradegenie.platform.tradegenie_backend_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableTransactionManagement
public class TradegenieBackendApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradegenieBackendApiApplication.class, args);
	}

}
