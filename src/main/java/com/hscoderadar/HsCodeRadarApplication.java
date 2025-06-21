package com.hscoderadar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * HS Code Radar 애플리케이션의 메인 클래스
 * 
 * <p>
 * 이 클래스는 Spring Boot 애플리케이션의 시작점이며, 다음 기능들을 활성화합니다:
 * <ul>
 * <li>JPA Auditing - 엔티티의 생성/수정 시간 자동 추적</li>
 * <li>스케줄링 - 주기적인 작업 실행</li>
 * <li>트랜잭션 관리 - 데이터베이스 트랜잭션 처리</li>
 * </ul>
 * 
 * @author Development Team
 * @since 1.0.0
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableTransactionManagement
public class HsCodeRadarApplication {

  /**
   * 애플리케이션 시작점
   * 
   * @param args 명령행 인수
   */
  public static void main(String[] args) {
    SpringApplication.run(HsCodeRadarApplication.class, args);
  }
}