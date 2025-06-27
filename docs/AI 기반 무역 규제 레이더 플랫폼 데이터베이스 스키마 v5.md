# AI 기반 무역 규제 레이더 플랫폼 데이터베이스 스키마 v5

## 📋 변경사항 요약 (v4.2 → v5)

### 🔄 **주요 변경사항**

1. **MySQL → PostgreSQL 17 전환**: RAG 시스템을 위한 pgvector 확장 지원
2. **JWT 무상태 인증 시스템**: 세션 기반 → 토큰 기반 인증으로 완전 전환
3. **SMS/이메일 통합 알림 시스템**: 일일 발송으로 사용자 편의성 향상
4. **벡터 DB 통합**: Langchain4j 1.1.0 beta7 최적화된 HSCode RAG 시스템
5. **토큰 블랙리스트 완전 제거**: JWT 무상태 특성을 활용한 시스템 단순화
6. **자동 알림 활성화**: 휴대폰 인증 시 기존 북마크 알림 자동 활성화

### 🎯 **변경 이유**

- **Spring Boot 3.5+ 최적화**: 최신 스프링 생태계와 완벽 호환
- **Langchain4j 1.1.0 beta7 통합**: 최신 RAG 시스템 지원
- **성능 향상**: PostgreSQL 관계형 + 벡터 검색 통합으로 쿼리 성능 대폭 향상
- **운영 단순화**: JWT 무상태 특성으로 세션 관리 복잡성 제거
- **사용자 경험**: 자동 알림 활성화로 설정 과정 단순화
- **확장성**: pgvector 기반 의미적 검색으로 HSCode 분류 정확도 향상

### 🔄 **기술 스택 업그레이드**

```
v4.2 → v5 기술 스택 변경
┌─────────────────────────────────────────────────────────────┐
│ MySQL 8.0 + RediSQL        → PostgreSQL 17 + pgvector      │
│ 세션 기반 인증             → JWT 무상태 인증                │
│ 즉시 SMS 알림             → 일일 SMS/이메일 통합 알림       │
│ 인메모리 HSCode 캐시       → pgvector 기반 RAG 시스템       │
│ Spring Session + RediSQL   → 순수 Redis 임시 데이터        │
└─────────────────────────────────────────────────────────────┘
결과: Spring Boot 3.5+ & Langchain4j 1.1.0 beta7 완전 최적화
```

---

## 1. PostgreSQL 17 데이터베이스 (🆕 v5 신규)

> 💡 v5 변경사항: MySQL에서 PostgreSQL 17로 완전 전환하여 pgvector 확장을 통한 RAG 시스템 지원

### 1.1 확장 설치 및 기본 설정

```sql
-- PostgreSQL 확장 설치
CREATE EXTENSION IF NOT EXISTS pgvector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS hstore;

-- 벡터 검색 최적화를 위한 설정
SET max_connections = 200;
SET shared_buffers = '256MB';
SET effective_cache_size = '1GB';
SET maintenance_work_mem = '64MB';

```

### 1.2 사용자 기본 정보 테이블

```sql
CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NULL COMMENT 'SNS 로그인 시 NULL 가능',
    name VARCHAR(100) NOT NULL,
    profile_image VARCHAR(500) NULL,
    phone_number VARCHAR(100) NULL COMMENT '인증된 휴대폰 번호 (AES-256 암호화)',
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    phone_verified_at TIMESTAMP NULL,
    -- JWT 무상태 인증을 위한 Refresh Token 관리
    refresh_token VARCHAR(500) NULL COMMENT '현재 유효한 리프레시 토큰',
    refresh_token_expires_at TIMESTAMP NULL COMMENT '리프레시 토큰 만료 시간',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
);

-- 인덱스 설정
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone_verified ON users(phone_verified);
CREATE INDEX idx_users_refresh_token ON users(refresh_token) WHERE refresh_token IS NOT NULL;
CREATE INDEX idx_users_created_at ON users(created_at);

-- 자동 업데이트 트리거
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at 
    BEFORE UPDATE ON users 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- 테이블 코멘트
COMMENT ON TABLE users IS '사용자 기본 정보 - JWT 무상태 인증 지원';
COMMENT ON COLUMN users.phone_number IS '휴대폰 번호 (AES-256 암호화 저장)';
COMMENT ON COLUMN users.refresh_token IS 'JWT Refresh Token (Token Rotation 지원)';

```

### 1.3 SNS 계정 연동 테이블

```sql
CREATE TYPE sns_provider AS ENUM ('GOOGLE', 'KAKAO', 'NAVER');

CREATE TABLE sns_accounts (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider sns_provider NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    provider_email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(provider, provider_id)
);

-- 인덱스 설정
CREATE INDEX idx_sns_accounts_user_id ON sns_accounts(user_id);
CREATE INDEX idx_sns_accounts_provider ON sns_accounts(provider, provider_id);

-- 업데이트 트리거
CREATE TRIGGER update_sns_accounts_updated_at 
    BEFORE UPDATE ON sns_accounts 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE sns_accounts IS 'SNS 계정 연동 정보';

```

### 1.4 사용자 설정 테이블 (SMS/이메일 통합)

```sql
CREATE TABLE user_settings (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    -- 통합 알림 설정 (SMS/이메일 동시 지원)
    sms_notification_enabled BOOLEAN NOT NULL DEFAULT FALSE COMMENT '전체 SMS 알림 활성화',
    email_notification_enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '전체 이메일 알림 활성화',
    -- 알림 발송 주기 설정 (개발자 제어)
    notification_frequency VARCHAR(20) NOT NULL DEFAULT 'DAILY' COMMENT '알림 주기: DAILY, WEEKLY',
    -- 알림 시간 설정
    notification_time TIME NOT NULL DEFAULT '09:00:00' COMMENT '일일 알림 발송 시간',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 업데이트 트리거
CREATE TRIGGER update_user_settings_updated_at 
    BEFORE UPDATE ON user_settings 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE user_settings IS '사용자 통합 알림 설정 (SMS/이메일)';

```

### 1.5 북마크 테이블 (자동 알림 활성화 지원)

```sql
CREATE TYPE bookmark_type AS ENUM ('HS_CODE', 'CARGO');

CREATE TABLE bookmarks (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type bookmark_type NOT NULL,
    target_value VARCHAR(50) NOT NULL COMMENT 'HS Code 또는 화물관리번호',
    display_name VARCHAR(200) NULL COMMENT '사용자 지정 표시명',
    -- 개별 북마크 알림 설정
    sms_notification_enabled BOOLEAN NOT NULL DEFAULT FALSE COMMENT '개별 SMS 알림',
    email_notification_enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '개별 이메일 알림',
    -- 자동 모니터링 상태 (알림 설정과 연동)
    monitoring_active BOOLEAN GENERATED ALWAYS AS (
        sms_notification_enabled OR email_notification_enabled
    ) STORED COMMENT '모니터링 활성화 상태 (알림 설정 기반 자동 계산)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, target_value)
);

-- 인덱스 설정
CREATE INDEX idx_bookmarks_user_id ON bookmarks(user_id);
CREATE INDEX idx_bookmarks_type ON bookmarks(type);
CREATE INDEX idx_bookmarks_monitoring_active ON bookmarks(monitoring_active) WHERE monitoring_active = true;
CREATE INDEX idx_bookmarks_target_value ON bookmarks(target_value);

-- 업데이트 트리거
CREATE TRIGGER update_bookmarks_updated_at 
    BEFORE UPDATE ON bookmarks 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE bookmarks IS '북마크 정보 - 자동 모니터링 및 알림 설정 통합';
COMMENT ON COLUMN bookmarks.monitoring_active IS '알림 설정 기반 자동 모니터링 상태';

```

### 1.6 업데이트 피드 테이블

```sql
CREATE TYPE feed_type AS ENUM (
    'HS_CODE_TARIFF_CHANGE',
    'HS_CODE_REGULATION_UPDATE', 
    'CARGO_STATUS_UPDATE',
    'TRADE_NEWS',
    'POLICY_UPDATE'
);

CREATE TYPE target_type AS ENUM ('HS_CODE', 'CARGO');
CREATE TYPE importance_level AS ENUM ('HIGH', 'MEDIUM', 'LOW');

CREATE TABLE update_feeds (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    feed_type feed_type NOT NULL,
    target_type target_type NULL,
    target_value VARCHAR(50) NULL COMMENT '대상 HS Code 또는 화물관리번호',
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    source_url VARCHAR(1000) NULL,
    importance importance_level NOT NULL DEFAULT 'MEDIUM',
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    -- 일일 알림 처리 상태
    included_in_daily_notification BOOLEAN NOT NULL DEFAULT FALSE COMMENT '일일 알림 포함 여부',
    daily_notification_sent_at TIMESTAMP NULL COMMENT '일일 알림 발송 시간',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 설정 (일일 알림 성능 최적화)
CREATE INDEX idx_update_feeds_user_id ON update_feeds(user_id);
CREATE INDEX idx_update_feeds_daily_notification ON update_feeds(user_id, included_in_daily_notification, created_at) WHERE included_in_daily_notification = false;
CREATE INDEX idx_update_feeds_importance ON update_feeds(importance);
CREATE INDEX idx_update_feeds_target ON update_feeds(target_type, target_value);

COMMENT ON TABLE update_feeds IS '업데이트 피드 - 일일 알림 시스템 지원';

```

### 1.7 통합 알림 로그 테이블 (SMS/이메일)

```sql
CREATE TYPE notification_type AS ENUM ('SMS', 'EMAIL');
CREATE TYPE notification_status AS ENUM ('PENDING', 'SENT', 'FAILED', 'DELIVERED');
CREATE TYPE message_type AS ENUM ('VERIFICATION', 'DAILY_NOTIFICATION', 'URGENT_ALERT');

CREATE TABLE notification_logs (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    notification_type notification_type NOT NULL,
    message_type message_type NOT NULL,
    recipient VARCHAR(255) NOT NULL COMMENT '휴대폰 번호 또는 이메일 주소',
    title VARCHAR(500) NULL COMMENT '이메일 제목 (SMS는 NULL)',
    content TEXT NOT NULL,
    status notification_status NOT NULL DEFAULT 'PENDING',
    -- 외부 서비스 연동 정보
    external_message_id VARCHAR(100) NULL COMMENT '외부 서비스 메시지 ID',
    error_message TEXT NULL,
    cost_krw INTEGER NULL COMMENT '발송 비용 (원 단위)',
    -- 발송 관련 시간 정보
    scheduled_at TIMESTAMP NULL COMMENT '예약 발송 시간',
    sent_at TIMESTAMP NULL,
    delivered_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 성능 최적화 인덱스
CREATE INDEX idx_notification_logs_user_type ON notification_logs(user_id, notification_type);
CREATE INDEX idx_notification_logs_status ON notification_logs(status, created_at);
CREATE INDEX idx_notification_logs_daily_batch ON notification_logs(message_type, scheduled_at) WHERE message_type = 'DAILY_NOTIFICATION';

COMMENT ON TABLE notification_logs IS 'SMS/이메일 통합 알림 발송 로그';

```

### 1.8 HSCode 벡터 저장소 (RAG 시스템)

```sql
-- HSCode RAG 시스템을 위한 벡터 저장소
CREATE TABLE hscode_vectors (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- HSCode 정보
    hscode VARCHAR(20) NOT NULL,
    product_name VARCHAR(500) NOT NULL,
    description TEXT NOT NULL,
    -- 벡터 임베딩 (voyage-3-large: 2048 차원)
    embedding VECTOR(2048) NOT NULL,
    -- RAG 메타데이터 (Langchain4j 호환)
    metadata JSONB NOT NULL DEFAULT '{}',
    -- 정확도 향상을 위한 추가 정보
    classification_basis TEXT NULL COMMENT 'HSCode 분류 근거',
    similar_hscodes JSONB NULL COMMENT '유사 HSCode 및 차이점',
    keywords TEXT[] NULL COMMENT '검색 키워드 배열',
    -- 품질 관리
    confidence_score FLOAT DEFAULT 0.0 COMMENT '분류 신뢰도 (0.0-1.0)',
    verified BOOLEAN DEFAULT FALSE COMMENT '전문가 검증 완료 여부',
    -- 시간 정보
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(hscode)
);

-- 벡터 검색 최적화 인덱스 (HNSW 알고리즘 사용)
CREATE INDEX idx_hscode_vectors_embedding ON hscode_vectors 
USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);

-- 일반 검색 인덱스
CREATE INDEX idx_hscode_vectors_hscode ON hscode_vectors(hscode);
CREATE INDEX idx_hscode_vectors_keywords ON hscode_vectors USING GIN(keywords);
CREATE INDEX idx_hscode_vectors_metadata ON hscode_vectors USING GIN(metadata);
CREATE INDEX idx_hscode_vectors_confidence ON hscode_vectors(confidence_score) WHERE confidence_score >= 0.8;

-- 업데이트 트리거
CREATE TRIGGER update_hscode_vectors_updated_at 
    BEFORE UPDATE ON hscode_vectors 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE hscode_vectors IS 'HSCode RAG 시스템 벡터 저장소 (Langchain4j 최적화)';
COMMENT ON COLUMN hscode_vectors.embedding IS 'voyage-3-large 벡터 (2048차원)';
COMMENT ON COLUMN hscode_vectors.metadata IS 'Langchain4j Document 메타데이터';

```

### 1.9 뉴스 테이블

```sql
CREATE TABLE news (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    source_url VARCHAR(1000) NOT NULL,
    source_name VARCHAR(200) NOT NULL,
    published_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 설정
CREATE INDEX idx_news_published_at ON news(published_at DESC);
CREATE INDEX idx_news_created_at ON news(created_at DESC);

COMMENT ON TABLE news IS '무역 뉴스 정보';

```

### 1.10 모니터링 로그 테이블 (후순위 구현)

```sql
-- Claude API 사용량 추적을 위한 모니터링 로그 (후순위 구현)
CREATE TABLE monitor_logs (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NULL REFERENCES users(id) ON DELETE SET NULL,
    api_endpoint VARCHAR(200) NOT NULL COMMENT '호출된 API 엔드포인트',
    claude_model VARCHAR(100) NOT NULL COMMENT '사용된 Claude 모델',
    input_tokens INTEGER NOT NULL DEFAULT 0 COMMENT '입력 토큰 수',
    output_tokens INTEGER NOT NULL DEFAULT 0 COMMENT '출력 토큰 수',
    total_cost_usd DECIMAL(10,6) NOT NULL DEFAULT 0.000000 COMMENT '총 비용 (USD)',
    response_time_ms INTEGER NOT NULL DEFAULT 0 COMMENT '응답 시간 (밀리초)',
    success BOOLEAN NOT NULL DEFAULT TRUE COMMENT '성공 여부',
    error_message TEXT NULL COMMENT '오류 메시지',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 비용 분석을 위한 인덱스
CREATE INDEX idx_monitor_logs_user_cost ON monitor_logs(user_id, created_at, total_cost_usd);
CREATE INDEX idx_monitor_logs_daily_stats ON monitor_logs(DATE(created_at), claude_model);

COMMENT ON TABLE monitor_logs IS 'Claude API 사용량 및 비용 모니터링 (후순위 구현)';

```

### 1.11 초기 설정 및 트리거

```sql
-- 사용자 생성 시 기본 설정 자동 생성
CREATE OR REPLACE FUNCTION create_user_default_settings()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO user_settings (user_id) VALUES (NEW.id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_create_user_default_settings
    AFTER INSERT ON users
    FOR EACH ROW
    EXECUTE FUNCTION create_user_default_settings();

-- 휴대폰 인증 완료 시 기존 북마크 알림 자동 활성화
CREATE OR REPLACE FUNCTION auto_activate_bookmark_notifications()
RETURNS TRIGGER AS $$
BEGIN
    -- 휴대폰 인증이 완료된 경우
    IF NEW.phone_verified = TRUE AND OLD.phone_verified = FALSE THEN
        -- 해당 사용자의 모든 북마크 SMS 알림 활성화
        UPDATE bookmarks 
        SET sms_notification_enabled = TRUE 
        WHERE user_id = NEW.id;
        
        -- 사용자 설정에서 SMS 알림 전체 활성화
        UPDATE user_settings 
        SET sms_notification_enabled = TRUE 
        WHERE user_id = NEW.id;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_auto_activate_bookmark_notifications
    AFTER UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION auto_activate_bookmark_notifications();

```

---

## 2. Redis 데이터 구조 (v5 단순화)

> 🔄 v5 변경사항: 세션 관리 제거, 순수 임시 데이터 저장소로 역할 단순화

### 2.1 SMS 인증 시스템

```
# SMS 인증 세션 정보
sms:verification:{verificationId}  # Hash
  ├── userId: {userId}
  ├── phoneNumber: {encryptedPhoneNumber}
  ├── verificationCode: {hashedCode}
  ├── attemptCount: {count}
  ├── maxAttempts: 5
  ├── isVerified: false
  ├── createdAt: {timestamp}
  └── TTL: 300초 (5분)

# 재발송 방지 쿨다운
sms:cooldown:{phoneNumber}         # String
  └── TTL: 120초 (2분)

# 일일 발송 한도 관리
sms:daily_limit:{phoneNumber}:{date}  # String
  ├── count: {발송횟수}
  └── TTL: 86400초 (24시간)

```

### 2.2 일일 알림 큐 시스템

```
# 일일 알림 처리 큐
daily_notification:queue:SMS       # List
daily_notification:queue:EMAIL     # List

# 알림 상세 정보
daily_notification:detail:{id}     # Hash
  ├── userId: {userId}
  ├── notificationType: SMS|EMAIL
  ├── recipient: {phoneNumber|email}
  ├── title: {title}
  ├── content: {content}
  ├── feedIds: [{feedId1}, {feedId2}, ...]
  ├── scheduledAt: {timestamp}
  ├── createdAt: {timestamp}
  └── TTL: 86400초 (24시간)

# 처리 중인 알림 추적
daily_notification:processing      # Set
daily_notification:counter         # String (카운터)

```

### 2.3 JWT 토큰 임시 저장 (필요시)

```
# JWT 토큰 임시 저장 (토큰 갱신 중 충돌 방지)
jwt:refresh_in_progress:{userId}   # String
  ├── refreshToken: {currentToken}
  └── TTL: 30초

# 임시 인증 상태 (필요시)
jwt:temp_auth:{tempToken}          # Hash
  ├── userId: {userId}
  ├── action: {requiredAction}
  └── TTL: 600초 (10분)

```

### 2.4 Redis 설정 (Spring Boot 3.5+)

```yaml
# application.yml
spring:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
        max-wait: -1ms
    database: 0

# 임시 데이터 정리 설정
app:
  redis:
    cleanup:
      enabled: true
      interval: 3600000  # 1시간마다
      expired-keys-scan-count: 100

```

### 2.5 Redis 사용 예시 (Java 코드)

```java
@Service
public class SMSVerificationService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String SMS_VERIFICATION_PREFIX = "sms:verification:";
    private static final String SMS_COOLDOWN_PREFIX = "sms:cooldown:";
    
    public void createVerificationSession(String verificationId, 
                                        Long userId, 
                                        String phoneNumber, 
                                        String code) {
        String key = SMS_VERIFICATION_PREFIX + verificationId;
        
        Map<String, Object> sessionData = Map.of(
            "userId", userId,
            "phoneNumber", encryptPhoneNumber(phoneNumber),
            "verificationCode", hashCode(code),
            "attemptCount", 0,
            "maxAttempts", 5,
            "isVerified", false,
            "createdAt", System.currentTimeMillis()
        );
        
        redisTemplate.opsForHash().putAll(key, sessionData);
        redisTemplate.expire(key, Duration.ofSeconds(300)); // 5분
        
        // 쿨다운 설정
        String cooldownKey = SMS_COOLDOWN_PREFIX + phoneNumber;
        redisTemplate.opsForValue()
                    .set(cooldownKey, "1", Duration.ofSeconds(120)); // 2분
    }
}

@Service
public class DailyNotificationService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    public void queueDailyNotification(Long userId, 
                                     String type, 
                                     String recipient,
                                     String title, 
                                     String content,
                                     List<Long> feedIds) {
        // 알림 ID 생성
        String notificationId = String.valueOf(
            redisTemplate.opsForValue().increment("daily_notification:counter")
        );
        
        // 상세 정보 저장
        String detailKey = "daily_notification:detail:" + notificationId;
        Map<String, Object> notificationData = Map.of(
            "userId", userId,
            "notificationType", type,
            "recipient", recipient,
            "title", title,
            "content", content,
            "feedIds", feedIds,
            "scheduledAt", System.currentTimeMillis(),
            "createdAt", System.currentTimeMillis()
        );
        
        redisTemplate.opsForHash().putAll(detailKey, notificationData);
        redisTemplate.expire(detailKey, Duration.ofDays(1)); // 24시간
        
        // 큐에 추가
        String queueKey = "daily_notification:queue:" + type;
        redisTemplate.opsForList().rightPush(queueKey, notificationId);
    }
}
```

---

## 3. 시스템 아키텍처 다이어그램 (v5)

```
┌─────────────────────────────────────────────────────────────┐
│                Spring Boot 3.5+ Application                 │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐    ┌─────────────────┐                │
│  │ JWT 무상태 인증  │    │ Langchain4j RAG │                │
│  │ (Spring Security│    │ (pgvector 통합) │                │
│  │     6.x)        │    │                 │                │
│  └─────────────────┘    └─────────────────┘                │
├─────────────────────────────────────────────────────────────┤
│                     Data Layer                              │
│  ┌─────────────────┐    ┌─────────────────┐                │
│  │ PostgreSQL 17   │    │   Redis 7.x     │                │
│  │ ┌─────────────┐ │    │ ┌─────────────┐ │                │
│  │ │사용자/북마크│ │    │ │SMS 인증     │ │                │
│  │ │알림 로그    │ │    │ │일일 알림 큐 │ │                │
│  │ │pgvector RAG │ │    │ │JWT 임시저장 │ │                │
│  │ └─────────────┘ │    │ └─────────────┘ │                │
│  └─────────────────┘    └─────────────────┘                │
└─────────────────────────────────────────────────────────────┘

```

---

## 4. 마이그레이션 가이드 (v4.2 → v5)

### 4.1 데이터 마이그레이션 매핑

| v4.2 (MySQL)    | v5 (PostgreSQL)     | 변경사항                                         |
| --------------- | ------------------- | -------------------------------------------- |
| `users`         | `users`             | `id` → `BIGINT GENERATED ALWAYS AS IDENTITY` |
| `user_settings` | `user_settings`     | SMS/이메일 통합 설정 추가                             |
| `bookmarks`     | `bookmarks`         | `monitoring_active` 자동 계산 컬럼 추가              |
| `sms_logs`      | `notification_logs` | SMS/이메일 통합 로그                                |
| `hscode_cache`  | `hscode_vectors`    | 벡터 임베딩 + RAG 메타데이터                           |
| RediSQL 세션      | 순수 Redis Hash       | 세션 시스템 제거                                    |

### 4.2 Langchain4j 1.1.0 beta7 설정

```java
// Maven 의존성 추가 필요:
// <dependency>
//     <groupId>dev.langchain4j</groupId>
//     <artifactId>langchain4j-voyage-ai</artifactId>
//     <version>1.1.0-beta7</version>
// </dependency>

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.voyageai.VoyageAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.time.Duration;

@Configuration
public class LangchainConfig {
    
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(
            @Autowired DataSource dataSource) {
        return PgVectorEmbeddingStore.builder()
                .dataSource(dataSource)
                .table("hscode_vectors")
                .dimension(2048) // voyage-3-large 2048 차원 지원
                .createTable(true)
                .dropTableFirst(false)
                .build();
    }
    
    @Bean
    public EmbeddingModel embeddingModel() {
        return VoyageAiEmbeddingModel.builder()
                .apiKey(System.getenv("VOYAGE_API_KEY")) // 환경변수 또는 application.yml에서 설정
                .modelName("voyage-3-large")
                .outputDimension(2048) // 2048 차원 명시적 설정 (기본값: 1024)
                .inputType("document") // document 또는 query 타입 지정 가능
                .timeout(Duration.ofSeconds(60))
                .maxRetries(3)
                .logRequests(false)
                .logResponses(false)
                .build();
    }
}

/*
 * voyage-3-large 모델 특징:
 * - 지원 차원: 256, 512, 1024(기본), 2048
 * - 컨텍스트 길이: 32,000 토큰
 * - Matryoshka 학습 지원으로 차원 유연성 제공
 * - 양자화 옵션: float(기본), int8, uint8, binary, ubinary
 * - OpenAI v3-large 대비 평균 9.74% 향상된 성능
 * 
 * API 키 설정 방법:
 * 1. 환경변수: export VOYAGE_API_KEY=your-api-key
 * 2. application.yml:
 *    voyage:
 *      api-key: ${VOYAGE_API_KEY:your-api-key}
 */
```

### 4.3 Spring Boot 3.5+ JWT 설정

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> 
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> 
                    oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())))
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/auth/**", "/api/chat/**").permitAll()
                    .anyRequest().authenticated())
                .build();
    }
    
    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(getSecretKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
```

---

## 5. 성능 최적화 권장사항

### 5.1 PostgreSQL 설정

```sql
-- 벡터 검색 성능 최적화
SET work_mem = '256MB';
SET maintenance_work_mem = '1GB';
SET random_page_cost = 1.1;

-- pgvector 인덱스 튜닝
CREATE INDEX CONCURRENTLY idx_hscode_vectors_embedding_custom 
ON hscode_vectors USING hnsw (embedding vector_cosine_ops) 
WITH (m = 32, ef_construction = 128);

-- 정기적인 통계 업데이트
ANALYZE hscode_vectors;

```

### 5.2 Redis 메모리 최적화

```
# redis.conf
maxmemory 512mb
maxmemory-policy allkeys-lru
save 900 1
save 300 10
save 60 10000

```

### 5.3 애플리케이션 레벨 최적화

```yaml
# application.yml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          batch_size: 25
          batch_versioned_data: true
        order_inserts: true
        order_updates: true

# 연결 풀 최적화
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

```

---

## 6. 보안 고려사항

### 6.1 데이터 암호화

```java
@Service
public class EncryptionService {
    
    private final AESUtil aesUtil;
    
    public String encryptPhoneNumber(String phoneNumber) {
        return aesUtil.encrypt(phoneNumber, getEncryptionKey());
    }
    
    public String decryptPhoneNumber(String encryptedPhoneNumber) {
        return aesUtil.decrypt(encryptedPhoneNumber, getEncryptionKey());
    }
    
    // 환경변수에서 암호화 키 로드
    private String getEncryptionKey() {
        return System.getenv("PHONE_ENCRYPTION_KEY");
    }
}
```

### 6.2 JWT 보안 강화

```java
@Component
public class JwtService {
    
    // 토큰 만료 시간 설정
    private static final long ACCESS_TOKEN_VALIDITY = 15 * 60 * 1000; // 15분
    private static final long REFRESH_TOKEN_VALIDITY = 7 * 24 * 60 * 60 * 1000; // 7일
    
    public String generateAccessToken(UserDetails userDetails) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_VALIDITY))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    // Refresh Token Rotation 구현
    public String rotateRefreshToken(String oldRefreshToken) {
        // 기존 토큰 무효화 및 새 토큰 생성
        // ...
    }
}
```

---

## 7. 모니터링 및 운영

### 7.1 헬스 체크

```java
@Component
public class CustomHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        // PostgreSQL 연결 상태
        boolean pgConnected = checkPostgreSQLConnection();
        
        // Redis 연결 상태  
        boolean redisConnected = checkRedisConnection();
        
        // pgvector 확장 상태
        boolean pgvectorEnabled = checkPgVectorExtension();
        
        if (pgConnected && redisConnected && pgvectorEnabled) {
            return Health.up()
                    .withDetail("postgresql", "Connected")
                    .withDetail("redis", "Connected")
                    .withDetail("pgvector", "Enabled")
                    .build();
        } else {
            return Health.down()
                    .withDetail("postgresql", pgConnected ? "Connected" : "Disconnected")
                    .withDetail("redis", redisConnected ? "Connected" : "Disconnected")
                    .withDetail("pgvector", pgvectorEnabled ? "Enabled" : "Disabled")
                    .build();
        }
    }
}
```

### 7.2 정리 작업 스케줄러

```java
@Component
public class DataCleanupScheduler {
    
    // 매일 새벽 2시 실행
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupExpiredData() {
        // 만료된 알림 로그 정리 (30일 이상)
        cleanupOldNotificationLogs();
        
        // 만료된 JWT 갱신 토큰 정리
        cleanupExpiredRefreshTokens();
        
        // Redis 만료 키 정리
        cleanupRedisExpiredKeys();
        
        // 벡터 DB 최적화
        optimizeVectorDatabase();
    }
    
    private void optimizeVectorDatabase() {
        // PostgreSQL VACUUM 및 ANALYZE 실행
        jdbcTemplate.execute("VACUUM ANALYZE hscode_vectors;");
    }
}
```

---

## 8. 주요 변경사항 체크리스트

### ✅ **완료된 변경사항**

- [x] MySQL → PostgreSQL 17 + pgvector 전환
- [x] JWT 무상태 인증 시스템 설계
- [x] SMS/이메일 통합 알림 시스템
- [x] 자동 알림 활성화 트리거
- [x] HSCode 벡터 RAG 시스템
- [x] 토큰 블랙리스트 제거
- [x] BIGINT GENERATED ALWAYS AS IDENTITY 적용
- [x] Langchain4j 1.1.0 beta7 최적화
- [x] 모니터링 활성화 자동 연동
- [x] 일일 알림 큐 시스템

### 🔄 **주요 개선사항**

1. **성능 향상**: 벡터 검색 + 관계형 쿼리 통합으로 응답 속도 향상
2. **운영 단순화**: JWT 무상태로 세션 관리 복잡성 제거
3. **사용자 경험**: 자동 알림 활성화로 설정 과정 단순화
4. **정확도 향상**: RAG 시스템으로 HSCode 분류 정확도 대폭 개선
5. **확장성**: pgvector 기반 의미적 검색으로 확장 가능

---

**🎯 v5 업데이트 완료: Spring Boot 3.5+ & Langchain4j 1.1.0 beta7 최적화된 PostgreSQL+pgvector 기반 차세대 무역 정보 플랫폼**