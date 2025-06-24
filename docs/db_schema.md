# AI 기반 무역 규제 레이더 플랫폼 데이터베이스 스키마 v4.1

## 개요
- **MySQL 8.0+ 기준**
- **ChatGPT 스타일 통합 채팅 및 SMS 알림 시스템 지원**
- **개선사항**: SNS 연동 강화, JPA 최적화, Spring Boot 배치 처리 지원

---

## 1. 사용자 관리 테이블

### 1.1 사용자 기본 정보 테이블

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE COMMENT '사용자 이메일 주소',
    password_hash VARCHAR(255) NULL COMMENT 'SNS 로그인 시 NULL 가능',
    name VARCHAR(100) NOT NULL COMMENT '사용자 표시명',
    profile_image VARCHAR(500) NULL COMMENT '프로필 이미지 URL (OAuth에서 자동 설정 또는 사용자 업로드)',
    phone_number VARCHAR(20) NULL COMMENT '인증된 휴대폰 번호 (암호화)',
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE COMMENT '휴대폰 인증 완료 여부',
    phone_verified_at TIMESTAMP NULL COMMENT '휴대폰 인증 완료 시간',
    refresh_token VARCHAR(500) NULL COMMENT '발급된 리프레시 토큰',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP NULL,
    INDEX idx_email (email),
    INDEX idx_phone_verified (phone_verified),
    INDEX idx_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT '사용자 기본 정보';
```

### 1.2 SNS 계정 연동 테이블 (강화됨 - 추가 연동 지원)

```sql
CREATE TABLE sns_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider ENUM('GOOGLE', 'KAKAO', 'NAVER') NOT NULL,
    provider_id VARCHAR(255) NOT NULL COMMENT 'SNS 제공업체의 사용자 ID',
    provider_email VARCHAR(255) NOT NULL COMMENT 'SNS 제공업체 이메일',
    provider_name VARCHAR(100) NOT NULL COMMENT 'SNS 제공업체 이름',
    is_primary BOOLEAN NOT NULL DEFAULT FALSE COMMENT '주 계정 여부 (회원가입시 사용한 SNS)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    UNIQUE KEY uk_provider_account (provider, provider_id),
    INDEX idx_user_id (user_id),
    INDEX idx_is_primary (is_primary)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT 'SNS 계정 연동 정보 (회원가입 및 추가 연동)';
```

### 1.3 사용자 설정 테이블 (SMS 알림 설정 포함)

```sql
CREATE TABLE user_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    push_notification_enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '푸시 알림 활성화',
    email_notification_enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '이메일 알림 활성화',
    sms_notification_enabled BOOLEAN NOT NULL DEFAULT FALSE COMMENT '전체 SMS 알림 활성화',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT '사용자 알림 설정';
```

---

## 2. SMS 인증 및 알림 시스템

### 2.1 SMS 인증 세션 테이블

```sql
CREATE TABLE sms_verification_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    verification_id VARCHAR(50) NOT NULL UNIQUE COMMENT '인증 세션 ID (verify_xxxxxxxxx)',
    user_id BIGINT NOT NULL COMMENT '인증 요청 사용자',
    phone_number VARCHAR(20) NOT NULL COMMENT '인증할 휴대폰 번호 (암호화)',
    verification_code VARCHAR(10) NOT NULL COMMENT '6자리 인증 코드 (해시)',
    attempt_count INT NOT NULL DEFAULT 0 COMMENT '인증 시도 횟수',
    max_attempts INT NOT NULL DEFAULT 5 COMMENT '최대 인증 시도 횟수',
    is_verified BOOLEAN NOT NULL DEFAULT FALSE COMMENT '인증 완료 여부',
    expires_at TIMESTAMP NOT NULL COMMENT '인증 코드 만료 시간 (5분)',
    cooldown_until TIMESTAMP NULL COMMENT '다음 발송 가능 시간 (2분)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    verified_at TIMESTAMP NULL COMMENT '인증 완료 시간',
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_verification_id (verification_id),
    INDEX idx_user_id (user_id),
    INDEX idx_expires_at (expires_at),
    INDEX idx_cooldown_until (cooldown_until)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT 'SMS 인증 세션 관리';
```

### 2.2 SMS 알림 설정 테이블 (북마크별, 알림 타입별)

```sql
CREATE TABLE sms_notification_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    notification_type ENUM(
        'TARIFF_CHANGE',
        'REGULATION_UPDATE',
        'CARGO_STATUS_UPDATE',
        'TRADE_NEWS'
    ) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '해당 타입 SMS 알림 활성화',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_notification_type (user_id, notification_type),
    INDEX idx_user_id (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT 'SMS 알림 타입별 설정';
```

### 2.3 SMS 발송 로그 테이블

```sql
CREATE TABLE sms_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    phone_number VARCHAR(20) NOT NULL COMMENT '발송 대상 휴대폰 번호 (마스킹)',
    message_type ENUM(
        'VERIFICATION',
        'NOTIFICATION'
    ) NOT NULL COMMENT '메시지 타입',
    content TEXT NOT NULL COMMENT '발송 내용',
    status ENUM(
        'PENDING',
        'SENT',
        'FAILED',
        'DELIVERED'
    ) NOT NULL DEFAULT 'PENDING',
    external_message_id VARCHAR(100) NULL COMMENT '외부 SMS 서비스 메시지 ID',
    error_message TEXT NULL COMMENT '발송 실패 시 에러 메시지',
    cost_krw INT NULL COMMENT '발송 비용 (원)',
    sent_at TIMESTAMP NULL COMMENT '발송 시간',
    delivered_at TIMESTAMP NULL COMMENT '전달 확인 시간',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_message_type (message_type),
    INDEX idx_status (status),
    INDEX idx_sent_at (sent_at),
    INDEX idx_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT 'SMS 발송 로그';
```

---

## 3. 채팅 기반 통합 API 시스템

### 3.1 채팅 작업 관리 테이블

```sql
CREATE TABLE chat_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id VARCHAR(50) NOT NULL UNIQUE COMMENT '채팅 작업 ID (job_chat_xxxxxxxxx)',
    session_token VARCHAR(50) NOT NULL UNIQUE COMMENT '일회용 세션 토큰 (UUID)',
    user_message TEXT NOT NULL COMMENT '사용자 원본 질문',
    claude_intent VARCHAR(50) NULL COMMENT 'Claude가 분석한 질의 의도',
    processing_status ENUM(
        'PENDING',
        'PROCESSING',
        'COMPLETED',
        'FAILED'
    ) NOT NULL DEFAULT 'PENDING',
    thinking_events JSON NULL COMMENT '사고과정 이벤트 목록',
    main_response TEXT NULL COMMENT '최종 응답 내용',
    detail_page_url VARCHAR(500) NULL COMMENT '상세 페이지 URL',
    sources JSON NULL COMMENT '참고 자료 소스 목록',
    related_info JSON NULL COMMENT '관련 정보 (HS Code, 카테고리 등)',
    estimated_time_seconds INT NULL COMMENT '예상 완료 시간 (초)',
    actual_time_seconds INT NULL COMMENT '실제 처리 시간 (초)',
    error_message TEXT NULL COMMENT '처리 실패 시 에러 메시지',
    token_expires_at TIMESTAMP NOT NULL COMMENT '토큰 만료 시간 (10분)',
    token_used_at TIMESTAMP NULL COMMENT '토큰 사용 시간 (일회용 처리)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL COMMENT '작업 완료 시간',
    INDEX idx_job_id (job_id),
    INDEX idx_session_token (session_token),
    INDEX idx_processing_status (processing_status),
    INDEX idx_token_expires_at (token_expires_at),
    INDEX idx_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT '채팅 작업 관리';
```

### 3.2 채팅 스트리밍 이벤트 로그 (선택사항)

```sql
CREATE TABLE chat_streaming_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id VARCHAR(50) NOT NULL,
    event_type VARCHAR(50) NOT NULL COMMENT 'thinking_* 또는 main_message_*',
    event_data JSON NOT NULL COMMENT '이벤트 데이터',
    sequence_number INT NOT NULL COMMENT '이벤트 순서',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_job_id (job_id),
    INDEX idx_event_type (event_type),
    INDEX idx_sequence_number (sequence_number),
    INDEX idx_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT '채팅 스트리밍 이벤트 로그';
```

---

## 4. 북마크 시스템 (v4.1 강화)

### 4.1 북마크 테이블 (표시명, 설명, SMS 설정 추가)

```sql
CREATE TABLE bookmarks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bookmark_id VARCHAR(20) NOT NULL UNIQUE COMMENT '북마크 고유 ID (bm_xxxxxxx)',
    user_id BIGINT NOT NULL,
    type ENUM('HS_CODE', 'CARGO') NOT NULL COMMENT '북마크 타입',
    target_value VARCHAR(50) NOT NULL COMMENT 'HS Code 또는 화물관리번호',
    display_name VARCHAR(100) NOT NULL COMMENT '사용자 지정 표시명',
    description TEXT NULL COMMENT '북마크 설명',
    monitoring_enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '모니터링 활성화',
    sms_notification_enabled BOOLEAN NOT NULL DEFAULT FALSE COMMENT '개별 SMS 알림 활성화',
    alert_count INT NOT NULL DEFAULT 0 COMMENT '받은 알림 개수',
    last_alert TIMESTAMP NULL COMMENT '마지막 알림 시간',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_bookmark (user_id, type, target_value),
    INDEX idx_bookmark_id (bookmark_id),
    INDEX idx_user_id (user_id),
    INDEX idx_type (type),
    INDEX idx_monitoring_enabled (monitoring_enabled),
    INDEX idx_sms_notification_enabled (sms_notification_enabled),
    INDEX idx_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT '북마크 정보';
```

---

## 5. 업데이트 피드 시스템 (v4.1 강화)

### 5.1 업데이트 피드 테이블 (중요도, 상세 변경사항 추가)

```sql
CREATE TABLE update_feeds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    feed_type ENUM(
        'HS_CODE_TARIFF_CHANGE',
        'HS_CODE_REGULATION_UPDATE',
        'CARGO_STATUS_UPDATE',
        'TRADE_NEWS',
        'POLICY_UPDATE'
    ) NOT NULL,
    target_type ENUM('HS_CODE', 'CARGO') NULL COMMENT 'HS Code 또는 화물 관련일 때',
    target_value VARCHAR(50) NULL COMMENT '대상 HS Code 또는 화물관리번호',
    title VARCHAR(500) NOT NULL COMMENT '피드 제목',
    content TEXT NOT NULL COMMENT '피드 내용',
    change_details JSON NULL COMMENT '변경사항 상세 정보 (이전값, 현재값, 적용일 등)',
    source_url VARCHAR(1000) NULL COMMENT '원본 소스 URL',
    importance ENUM('HIGH', 'MEDIUM', 'LOW') NOT NULL DEFAULT 'MEDIUM' COMMENT '중요도',
    bookmark_id VARCHAR(20) NULL COMMENT '연관된 북마크 ID',
    is_read BOOLEAN NOT NULL DEFAULT FALSE COMMENT '읽음 상태',
    sms_notification_sent BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'SMS 알림 발송 여부',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_feed_type (feed_type),
    INDEX idx_target_type (target_type),
    INDEX idx_target_value (target_value),
    INDEX idx_importance (importance),
    INDEX idx_bookmark_id (bookmark_id),
    INDEX idx_is_read (is_read),
    INDEX idx_sms_notification_sent (sms_notification_sent),
    INDEX idx_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT '업데이트 피드';
```

---

## 6. 뉴스 및 알림 시스템

### 6.1 뉴스 테이블 (AI 요약, 만료일 관리)

```sql
CREATE TABLE news (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type ENUM('GENERAL', 'HS_CODE_SPECIFIC') NOT NULL COMMENT '뉴스 타입',
    hsCode VARCHAR(20) NULL COMMENT 'HS_CODE_SPECIFIC 타입일 때만 사용',
    title VARCHAR(500) NOT NULL COMMENT '뉴스 제목',
    content TEXT NOT NULL COMMENT '뉴스 내용',
    summary TEXT NULL COMMENT 'AI가 생성한 요약',
    sourceUrl VARCHAR(1000) NOT NULL COMMENT '원본 뉴스 URL',
    sourceName VARCHAR(200) NOT NULL COMMENT '뉴스 소스명',
    publishedAt TIMESTAMP NOT NULL COMMENT '뉴스 발행 시간',
    expires_at TIMESTAMP NOT NULL COMMENT '자동 삭제 시간 (1주일 후)',
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_type (type),
    INDEX idx_hsCode (hsCode),
    INDEX idx_expires_at (expires_at),
    INDEX idx_publishedAt (publishedAt),
    INDEX idx_createdAt (createdAt)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT '무역 뉴스';
```

### 6.2 알림 테이블 (푸시, 이메일 알림) - feed_id 제거됨

```sql
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    notification_type ENUM('PUSH', 'EMAIL') NOT NULL COMMENT '알림 타입',
    title VARCHAR(500) NOT NULL COMMENT '알림 제목',
    content TEXT NOT NULL COMMENT '알림 내용',
    is_sent BOOLEAN NOT NULL DEFAULT FALSE COMMENT '발송 완료 여부',
    sent_at TIMESTAMP NULL COMMENT '발송 시간',
    error_message TEXT NULL COMMENT '발송 실패 시 에러 메시지',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_notification_type (notification_type),
    INDEX idx_is_sent (is_sent),
    INDEX idx_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT '푸시/이메일 알림';
```

---

## 7. 캐시 테이블 (v4.1 간소화)

### 7.1 HS Code 캐시 테이블 (간소화됨)

```sql
CREATE TABLE hscode_cache (
    hscode VARCHAR(20) NOT NULL PRIMARY KEY COMMENT 'HS Code',
    product_name VARCHAR(255) NULL COMMENT '품목명',
    description TEXT NULL COMMENT '상세 설명'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT 'HS Code 정보 캐시 (간소화)';
```

---

## 8. 트리거 및 초기 설정

### 8.1 사용자 생성 시 기본 설정 자동 생성

```sql
DELIMITER //

CREATE TRIGGER tr_users_after_insert
AFTER INSERT ON users
FOR EACH ROW
BEGIN
    -- 기본 사용자 설정 생성
    INSERT INTO user_settings (user_id) VALUES (NEW.id);
    
    -- 기본 SMS 알림 설정 생성 (모든 타입 비활성화)
    INSERT INTO sms_notification_settings (user_id, notification_type, enabled) VALUES
    (NEW.id, 'TARIFF_CHANGE', FALSE),
    (NEW.id, 'REGULATION_UPDATE', FALSE),
    (NEW.id, 'CARGO_STATUS_UPDATE', FALSE),
    (NEW.id, 'TRADE_NEWS', FALSE);
END//

DELIMITER ;
```

### 8.2 북마크 생성 시 북마크 ID 자동 생성

```sql
DELIMITER //

CREATE TRIGGER tr_bookmarks_before_insert
BEFORE INSERT ON bookmarks
FOR EACH ROW
BEGIN
    IF NEW.bookmark_id IS NULL OR NEW.bookmark_id = '' THEN
        SET NEW.bookmark_id = CONCAT('bm_', LPAD(NEW.id, 6, '0'));
    END IF;
END//

DELIMITER ;
```

### 8.3 피드 생성 시 북마크 알림 카운트 업데이트

```sql
DELIMITER //

CREATE TRIGGER tr_update_feeds_after_insert
AFTER INSERT ON update_feeds
FOR EACH ROW
BEGIN
    -- 연관된 북마크의 alert_count 증가
    IF NEW.bookmark_id IS NOT NULL THEN
        UPDATE bookmarks 
        SET alert_count = alert_count + 1,
            last_alert = NOW()
        WHERE bookmark_id = NEW.bookmark_id;
    END IF;
END//

DELIMITER ;
```

---

## 9. 성능 최적화 인덱스

### 9.1 복합 인덱스 (자주 사용되는 쿼리 패턴)

```sql
CREATE INDEX idx_bookmarks_user_monitoring ON bookmarks (user_id, monitoring_enabled);
CREATE INDEX idx_feeds_user_unread ON update_feeds (user_id, is_read);
CREATE INDEX idx_feeds_user_importance ON update_feeds (user_id, importance);
CREATE INDEX idx_sms_logs_user_status ON sms_logs (user_id, status);
CREATE INDEX idx_chat_jobs_status_created ON chat_jobs (processing_status, created_at);
```

### 9.2 SNS 연동 관련 인덱스

```sql
CREATE INDEX idx_sns_accounts_user_primary ON sns_accounts (user_id, is_primary);
```

---

## 10. 샘플 데이터 (개발 환경용) - 수정됨

### 10.1 테스트 사용자 생성 (registration_type 제거됨)

```sql
INSERT INTO users (email, name)
VALUES 
    ('test@example.com', '테스트사용자'),
    ('admin@example.com', '관리자');
```

### 10.2 SNS 연동 샘플 데이터 (SNS 회원가입 사용자)

```sql
INSERT INTO users (email, name, profile_image)
VALUES ('sns_user@example.com', 'SNS사용자', 'https://example.com/profile.jpg');

-- 마지막으로 생성된 사용자 ID 가져와서 SNS 계정 연동
INSERT INTO sns_accounts (user_id, provider, provider_id, provider_email, provider_name, is_primary)
VALUES (
    (SELECT id FROM users WHERE email = 'sns_user@example.com'),
    'GOOGLE',
    'google_123456789',
    'sns_user@gmail.com',
    'SNS사용자',
    TRUE
);
```

### 10.3 샘플 HS Code 캐시 데이터 (간소화됨)

```sql
INSERT INTO hscode_cache (hscode, product_name, description)
VALUES 
    ('1905.90.90', '기타 베이커리 제품', '냉동피자 등 기타 베이커리 제품'),
    ('8517.12.00', '무선전화기', '스마트폰 및 기타 무선전화기'),
    ('2202.10.00', '무알코올 음료', '에너지드링크 등 무알코올 음료');
```

---

## 11. 스키마 버전 정보

```sql
CREATE TABLE schema_version (
    version VARCHAR(10) NOT NULL PRIMARY KEY,
    description TEXT,
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO schema_version (version, description)
VALUES (
    '4.1.0',
    'SNS 연동 강화, JPA 최적화, Spring Boot 배치 처리 지원'
);
```

---

## 주요 변경사항 요약 (v4.0 → v4.1)

### ❌ 삭제된 요소들
- **system_logs** 테이블 완전 삭제
- **정리 작업 이벤트 스케줄러** 4개 삭제 (Spring Boot에서 처리)
- **뷰 테이블** 2개 삭제 (JPA 사용으로 불필요)
- **users.registration_type** 컬럼 삭제
- **notifications.feed_id** 컬럼 삭제

### 🔧 hscode_cache 테이블 간소화
- **id** 컬럼 삭제, **hscode**를 PRIMARY KEY로 변경
- **tradeStats, comtradeData, tariffInfo, regulationInfo, lastUpdated, expires_at** 컬럼 삭제
- 핵심 정보만 유지: `hscode`, `product_name`, `description`

### 🔗 SNS 연동 시스템 강화
- **sns_accounts** 테이블에 **is_primary** 컬럼 추가
- 일반 회원가입 후 SNS 추가 연동 지원
- **사용자 분류는 sns_accounts 테이블 존재 여부로 판단**

### ⚡ Spring Boot 최적화
- JPA 환경에 맞는 구조 조정
- 배치 처리를 위한 이벤트 스케줄러 제거
- 뷰 대신 JPA 쿼리 메서드 사용

### 📈 인덱스 최적화
- SNS 연동 관련 복합 인덱스 추가
- 성능 최적화 인덱스 유지

### 📊 샘플 데이터 수정
- registration_type 참조 제거
- SNS 연동 샘플 데이터 추가
- system_logs 참조 제거

---

## 🔧 활용 가이드

### SNS 연동 판단 로직 (JPA 예시)

```java
// 사용자가 SNS로 가입했는지 확인
boolean isSnsUser = !user.getSnsAccounts().isEmpty();

// 주 SNS 계정 조회 (최초 가입시 사용한 SNS)
Optional<SnsAccount> primarySns = user.getSnsAccounts()
    .stream()
    .filter(SnsAccount::getIsPrimary)
    .findFirst();
```

이제 Spring Boot 환경에서 배치 처리로 데이터 정리 작업을 수행하고, JPA를 통해 효율적인 데이터 관리가 가능합니다!