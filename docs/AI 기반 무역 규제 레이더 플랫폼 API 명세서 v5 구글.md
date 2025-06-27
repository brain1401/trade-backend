

## **🚀 주요 변경사항 (v4.2 → v5)**

### ✨ **PostgreSQL + pgvector RAG 시스템 도입 (혁신적 개선)**

  - **데이터베이스 전환**: MySQL → PostgreSQL 17 + pgvector 확장
  - **RAG 시스템**: Langchain4j 1.1.0 beta7 기반 HSCode 벡터 검색
  - **정확도 향상**: 의미적 검색으로 HSCode 분류 정확도 대폭 개선
  - **Spring Boot 3.5+ 최적화**: 최신 기술 스택 완전 지원

### 📧 **SMS/이메일 통합 알림 시스템 (v5 핵심 기능)**

  - **통합 알림**: SMS 단독 → SMS/이메일 동시 발송
  - **일일 알림**: 즉시 알림 → 하루마다 변동사항 요약 발송
  - **자동 활성화**: 휴대폰 인증 완료 시 기존 북마크 SMS/이메일 알림 자동 활성화
  - **통합 관리**: 대시보드에서 SMS/이메일 알림 통합 설정

### 🔧 **핵심 변경 로직**

```
기존: SMS 단독 + 즉시 알림 + 사용자 동의 필요
변경: SMS/이메일 통합 + 일일 알림 + 자동 활성화
```

### 🎯 **병렬 처리 시스템 (v5 혁신)**

  - **자연어 응답**: Claude AI 분석 결과를 실시간 스트리밍
  - **상세페이지 준비**: HSCode 캐시/벡터 검색을 통한 상세 정보 병렬 생성
  - **로딩 최적화**: 상세페이지 버튼에 로딩 스피너 → 준비 완료 시 버튼 활성화

-----

## 1. 개요 (Overview)

본 문서는 'AI 기반 무역 규제 레이더 플랫폼 v5'의 RESTful API를 상세히 기술한 통합 명세서입니다. 이 플랫폼은 복잡한 무역 규제, HS Code 분류, 수출입 요건, 화물 추적 등 무역 업무에 필수적인 정보들을 AI를 통해 분석하고 사용자에게 실시간으로 제공하는 것을 목표로 합니다.

### 1.1 시스템 아키텍처 및 인증 방식

### ChatGPT 스타일 통합 아키텍처 + RAG 시스템

```
┌─────────────────────────────────────────────────────────┐
│              ChatGPT 스타일 통합 채팅 + RAG              │
│                                                         │
│  POST /api/chat → Claude 분석 → 즉시 SSE 스트리밍 시작    │
│        ↓                    ↓                           │
│  [자연어 응답]        [병렬: 상세페이지 준비]              │
│   실시간 스트리밍      HSCode 벡터 검색                   │
│        ↓                    ↓                           │
│  ┌─ Thinking 영역 ─┐  ┌─ Main Message 영역 ─┐            │
│  │ AI 사고과정 표시 │  │ 최종 통합 답변      │            │
│  └─────────────────┘  └───────────────────┘            │
│                                ↓                       │
│                 [상세페이지 이동 버튼]                    │
│              (의도 기반 우선순위 자동 정렬)               │
└─────────────────────────────────────────────────────────┘
                      │
              ┌─────────────────────────────┐
              │ Private API (북마크/대시보드) │
              │ --------------------------- │
              │ Access Token (Bearer)     │
              │ Refresh Token (HttpOnly)  │
              └─────────────────────────────┘
```

### 혁신적 설계 원칙 (v5)

  - **RAG-Powered Intelligence**: PostgreSQL+pgvector 기반 의미적 검색
  - **Parallel Processing**: 자연어 응답과 상세페이지 정보 병렬 생성
  - **Smart Notification**: SMS/이메일 통합 일일 알림 시스템
  - **Auto-Activation**: 휴대폰 인증 시 기존 북마크 알림 자동 활성화
  - **Modern Authentication**: Bearer Token(Access) + HttpOnly Cookie(Refresh) 분리

### v5 단순화된 데이터 플로우

1.  **통합 질의**: 자연어 질문 → Claude 분석 → 즉시 스트리밍 시작
2.  **병렬 처리**: [자연어 응답 스트리밍] + [HSCode 벡터 검색 → 상세페이지 준비]
3.  **개인 데이터**: 북마크 → 일일 모니터링 → SMS/이메일 통합 알림 발송 (인증 필수)

### 1.2 기본 정보

  - **기본 URL**: `http://localhost:8081/api`
  - **프로토콜**: HTTPS (운영환경)
  - **인증 방식**: Bearer Token 인증 (Access Token) + HttpOnly Cookie (Refresh Token)
  - **Content-Type**: `application/json`
  - **CORS**: Public API는 기본 허용, Private API는 `withCredentials: true` 필수

### 1.3 공통 응답 형식 (Common Response Wrapper)

모든 API 응답은 아래와 같은 `ApiResponse` 객체로 래핑되어 반환됩니다.

```typescript
type ApiResponse<T> = {
  success: 'SUCCESS' | 'ERROR';
  message: string;
  data: T | null;
}
```

### 성공 응답 예시

```json
{
  "success": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "intent": "HS_CODE_ANALYSIS",
    "hsCode": "8517.12.00",
    "description": "스마트폰 및 기타 무선전화기"
  }
}
```

### 오류 응답 예시

```json
{
  "success": "ERROR",
  "message": "검색어가 비어있습니다.",
  "data": null
}
```

-----

## 2. 인증 API (Authentication) - v5.1 Access/Refresh 분리 모델

### 2.1 회원가입

**`POST /api/auth/register`**

신규 계정을 생성합니다. 생성된 계정은 즉시 로그인 가능한 상태가 됩니다.

### 📊 응답 코드 매트릭스

| 시나리오 | HTTP 상태 | 에러 코드 | 응답 메시지 |
|---|---|---|---|
| ✅ 성공 | `201 Created` | - | "계정이 생성되었습니다" |
| ❌ 이메일 중복 | `409 Conflict` | USER_001 | "이미 사용 중인 이메일입니다" |
| ❌ 입력 데이터 오류 | `400 Bad Request` | USER_002 | "입력 정보가 올바르지 않습니다" |
| ❌ 비밀번호 정책 위반 | `422 Unprocessable Entity` | USER_004 | "비밀번호가 정책에 맞지 않습니다" |
| ❌ 서버 오류 | `500 Internal Server Error` | COMMON_002 | "서버에서 오류가 발생했습니다" |

### Request Body

| 필드명 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `email` | string | ✓ | 사용자 이메일 주소 |
| `password` | string | ✓ | 사용자 비밀번호 (최소 8자 이상) |
| `name` | string | ✓ | 사용자 표시명 (환영 메시지 등에 사용) |

```json
{
  "email": "user@example.com",
  "password": "password123",
  "name": "홍길동"
}
```

### Response (201 Created)

| 필드명 | 타입 | 설명 |
|---|---|---|
| `success` | string | 요청 처리 결과 ("SUCCESS" 또는 "ERROR") |
| `message` | string | 처리 결과 메시지 |
| `data.email` | string | 사용자 이메일 주소 |
| `data.name` | string | 사용자 표시명 |
| `data.profileImage` | string | 프로필 이미지 URL (회원가입 시에는 null) |

```json
{
  "success": "SUCCESS",
  "message": "계정이 생성되었습니다",
  "data": {
    "email": "user@example.com",
    "name": "홍길동",
    "profileImage": null
  }
}
```

-----

### 2.2 로그인

**`POST /api/auth/login`**

사용자 인증을 수행하고, 성공 시 **Access Token은 응답 본문에, Refresh Token은 HttpOnly 쿠키에** 설정합니다.

### 📊 응답 코드 매트릭스

| 시나리오 | HTTP 상태 | 에러 코드 | 응답 메시지 |
|---|---|---|---|
| ✅ 성공 | `200 OK` | - | "인증되었습니다" |
| ❌ 등록되지 않은 사용자 | `401 Unauthorized` | AUTH_001 | "이메일 또는 비밀번호가 올바르지 않습니다" |
| ❌ 비밀번호 불일치 | `401 Unauthorized` | AUTH_001 | "이메일 또는 비밀번호가 올바르지 않습니다" |
| ❌ 계정 잠김 | `423 Locked` | AUTH_002 | "현재 계정에 일시적인 접근 제한이 적용되었습니다" |
| ❌ 입력 데이터 누락 | `400 Bad Request` | COMMON_001 | "필수 입력 정보가 누락되었습니다" |
| ❌ 너무 많은 시도 | `429 Too Many Requests` | RATE_LIMIT_001 | "로그인 시도 한도를 초과했습니다" |

### Request Body

| 필드명 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `email` | string | ✓ | 사용자 이메일 주소 |
| `password` | string | ✓ | 사용자 비밀번호 |
| `rememberMe` | boolean | - | 로그인 유지 여부 (기본값: false) |

```json
{
  "email": "user@example.com",
  "password": "password123",
  "rememberMe": true
}
```

### Response (200 OK)

| 필드명 | 타입 | 설명 |
|---|---|---|
| `success` | string | 요청 처리 결과 ("SUCCESS" 또는 "ERROR") |
| `message` | string | 처리 결과 메시지 |
| `data.accessToken` | string | API 인증에 사용하는 Access Token |
| `data.user.email` | string | 사용자 이메일 주소 |
| `data.user.name` | string | 사용자 표시명 |
| `data.user.profileImage` | string | 프로필 이미지 URL (없으면 null) |

```json
{
  "success": "SUCCESS",
  "message": "인증되었습니다",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "email": "user@example.com",
      "name": "홍길동",
      "profileImage": null
    }
  }
}
```

### Response Headers (Set-Cookie) - Refresh Token

인증 성공 시 Refresh Token이 담긴 HttpOnly 쿠키가 자동으로 설정됩니다:

```
Set-Cookie: refresh_token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...;
            HttpOnly;
            Secure;
            SameSite=Strict;
            Path=/api/auth/refresh;
            Max-Age=1209600
```

### v5.1 토큰 설정 정책

  - **Access Token**: 유효기간 1시간, 클라이언트 메모리에 저장.
  - **Refresh Token**:
      - `rememberMe: true` → `Max-Age=1209600` (14일간 유효)
      - `rememberMe: false` → 세션 쿠키 (브라우저 종료시 삭제)
  - **보안**: `Path` 속성을 토큰 갱신 API 경로로 한정하여 불필요한 전송 방지.

-----

### 2.3 인증 상태 확인

**`GET /api/auth/verify`**

현재 Access Token의 유효성을 확인하고 사용자 정보를 반환합니다.

### 📊 응답 코드 매트릭스

| 시나리오 | HTTP 상태 | 에러 코드 | 응답 메시지 |
|---|---|---|---|
| ✅ 유효한 토큰 | `200 OK` | - | "인증 상태 확인됨" |
| ❌ 토큰 만료 | `401 Unauthorized` | AUTH_003 | "인증이 만료되었습니다" |
| ❌ 유효하지 않은 토큰 | `401 Unauthorized` | AUTH_004 | "인증 정보가 올바르지 않습니다" |
| ❌ 토큰 없음 | `401 Unauthorized` | AUTH_004 | "인증이 필요합니다" |

### Request Headers

`Authorization` 헤더에 Access Token을 담아 전송합니다.

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Response (200 OK)

| 필드명 | 타입 | 설명 |
|---|---|---|
| `success` | string | 요청 처리 결과 ("SUCCESS" 또는 "ERROR") |
| `message` | string | 처리 결과 메시지 |
| `data.email` | string | 사용자 이메일 주소 |
| `data.name` | string | 사용자 표시명 |
| `data.profileImage` | string | 프로필 이미지 URL (없으면 null) |
| `data.phoneVerified` | boolean | 휴대폰 인증 완료 여부 |

```json
{
  "success": "SUCCESS",
  "message": "인증 상태 확인됨",
  "data": {
    "email": "user@example.com",
    "name": "홍길동",
    "profileImage": null,
    "phoneVerified": false
  }
}
```

-----

### 2.4 OAuth 소셜 로그인

**`GET /api/oauth2/authorization/{provider}`**

외부 OAuth 제공업체를 통한 소셜 로그인을 시작합니다.

### 📊 응답 코드 매트릭스

| 시나리오 | HTTP 상태 | 에러 코드 | 응답 메시지 |
|---|---|---|---|
| ✅ 리디렉션 시작 | `302 Found` | - | OAuth 제공자로 리디렉션 |
| ❌ 지원하지 않는 제공자 | `400 Bad Request` | OAUTH_001 | "지원하지 않는 OAuth 제공자입니다" |
| ❌ OAuth 인증 실패 | `401 Unauthorized` | OAUTH_002 | "소셜 로그인에 실패했습니다" |
| ❌ OAuth 취소 | `400 Bad Request` | OAUTH_003 | "사용자가 인증을 취소했습니다" |

### Path Parameters

| 필드명 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `provider` | string | ✓ | OAuth 제공업체 (`google`, `naver`, `kakao`) |

### Query Parameters

| 필드명 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `rememberMe` | boolean | - | 로그인 유지 여부 (기본값: false) |

### Response (302 Found)

사용자를 해당 OAuth 제공업체의 인증 페이지로 리디렉션합니다.

```
Location: https://accounts.google.com/oauth/authorize?client_id=...
```

### OAuth 성공 시 콜백

인증 성공 시 다음 작업이 수행됩니다:

1.  사용자 정보 획득 (이메일, 이름, 프로필 이미지)
2.  **Access Token은 쿼리 파라미터로, Refresh Token은 HttpOnly 쿠키로 설정**
3.  프론트엔드로 리디렉션

```
Location: https://your-frontend-domain.com/auth/callback?success=true&accessToken=eyJhbGci...
Set-Cookie: refresh_token=eyJhbGci...; HttpOnly; ...
```

### OAuth 실패 시 콜백

```
Location: https://your-frontend-domain.com/auth/callback?error=oauth_failed
```

-----

### 2.5 JWT 토큰 갱신

**`POST /api/auth/refresh`**

HttpOnly 쿠키에 담긴 Refresh Token을 사용하여 새로운 Access Token을 발급받습니다.

### 📊 응답 코드 매트릭스

| 시나리오 | HTTP 상태 | 에러 코드 | 응답 메시지 |
|---|---|---|---|
| ✅ 갱신 성공 | `200 OK` | - | "토큰이 갱신되었습니다" |
| ❌ 쿠키 없음 | `401 Unauthorized` | AUTH_005 | "Refresh Token 쿠키가 필요합니다" |
| ❌ 유효하지 않은 토큰 | `401 Unauthorized` | AUTH_003 | "유효하지 않은 Refresh Token입니다" |
| ❌ 만료된 토큰 | `401 Unauthorized` | AUTH_003 | "만료된 Refresh Token입니다" |
| ❌ DB 토큰 불일치 | `401 Unauthorized` | AUTH_004 | "인증 정보가 일치하지 않습니다" |

### Request Headers

브라우저에서 자동으로 전송되는 Refresh Token HttpOnly 쿠키를 사용합니다. (클라이언트에서 별도 처리 불필요)

```
Cookie: refresh_token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Response (200 OK)

| 필드명 | 타입 | 설명 |
|---|---|---|
| `success` | string | 요청 처리 결과 ("SUCCESS" 또는 "ERROR") |
| `message` | string | 처리 결과 메시지 |
| `data.accessToken` | string | 새로 발급된 Access Token |

```json
{
  "success": "SUCCESS",
  "message": "토큰이 갱신되었습니다",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

### Response Headers (Set-Cookie) - Token Rotation

보안 강화를 위해 토큰 갱신 시 새로운 Refresh Token을 발급하여 기존 토큰을 교체(Rotation)할 수 있습니다.

```
Set-Cookie: refresh_token=...; HttpOnly; ...
```

### v5.1 Token Rotation 보안 정책

  - **기존 Refresh Token 무효화**: 새 토큰 발급과 동시에 기존 토큰 즉시 폐기
  - **PostgreSQL 검증**: 요청한 토큰이 데이터베이스에 저장된 토큰과 일치하는지 검증
  - **재사용 방지**: 이미 사용된 Refresh Token으로는 갱신 불가

-----

### 2.6 로그아웃

**`POST /api/auth/logout`**

현재 세션을 종료하고 Refresh Token 쿠키를 삭제합니다.

### 📊 응답 코드 매트릭스

| 시나리오 | HTTP 상태 | 에러 코드 | 응답 메시지 |
|---|---|---|---|
| ✅ 성공 | `204 No Content` | - | 응답 본문 없음 |
| ✅ 이미 로그아웃 상태 | `200 OK` | - | "이미 로그아웃 상태입니다" |

### Request Headers

로그아웃할 사용자를 식별하기 위해 Access Token을 전송해야 합니다.

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Response Headers (Set-Cookie)

로그아웃 시 Refresh Token 쿠키가 자동으로 삭제됩니다:

```
Set-Cookie: refresh_token=; HttpOnly; Secure; SameSite=Strict; Path=/api/auth/refresh; Max-Age=0
```

-----

## 3. SMS/이메일 통합 알림 시스템 v5 🔒 PRIVATE API

> 🔐 인증 필수: 이 섹션의 모든 API는 **`Authorization: Bearer <Access Token>`** 헤더가 필요합니다.
>  
> 🚀 **v5 핵심 기능**: 휴대폰 인증 시 기존 북마크 SMS/이메일 알림 자동 활성화 + 일일 통합 알림

### 3.1 휴대폰 인증 코드 발송

**`POST /api/notification/phone/send`**

휴대폰 번호 인증을 위한 인증 코드를 발송합니다.

### 📊 응답 코드 매트릭스

| 시나리오 | HTTP 상태 | 에러 코드 | 응답 메시지 |
|---|---|---|---|
| ✅ 발송 성공 | `200 OK` | - | "인증 코드가 발송되었습니다" |
| ❌ 인증 필요 | `401 Unauthorized` | AUTH_003 | "인증이 필요합니다" |
| ❌ 잘못된 번호 형식 | `400 Bad Request` | SMS_001 | "휴대폰 번호 형식이 올바르지 않습니다" |
| ❌ 이미 인증된 번호 | `409 Conflict` | SMS_002 | "이미 인증된 휴대폰 번호입니다" |
| ❌ 발송 한도 초과 | `429 Too Many Requests` | SMS_003 | "인증 코드 발송 한도를 초과했습니다" |
| ❌ SMS 서비스 오류 | `502 Bad Gateway` | SMS_004 | "SMS 발송 서비스에 문제가 발생했습니다" |

### Authentication (Required)

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Request Body

| 필드명 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `phoneNumber` | string | ✓ | 휴대폰 번호 (010-0000-0000 또는 01000000000) |

```json
{
  "phoneNumber": "010-1234-5678"
}
```

### Response (200 OK)

| 필드명 | 타입 | 설명 |
|---|---|---|
| `success` | string | 요청 처리 결과 ("SUCCESS" 또는 "ERROR") |
| `message` | string | 처리 결과 메시지 |
| `data.verificationId` | string | 인증 세션 ID |
| `data.expiresAt` | string | 인증 코드 만료 시간 (ISO 8601) |
| `data.cooldownUntil` | string | 다음 발송 가능 시간 (ISO 8601) |
| `data.autoActivationWarning` | string | v5: 자동 알림 활성화 안내 메시지 |

```json
{
  "success": "SUCCESS",
  "message": "인증 코드가 발송되었습니다",
  "data": {
    "verificationId": "verify_123456789",
    "expiresAt": "2024-01-16T10:35:00Z",
    "cooldownUntil": "2024-01-16T10:33:00Z",
    "autoActivationWarning": "휴대폰 인증 완료 시 기존 북마크의 SMS/이메일 알림이 자동으로 활성화됩니다"
  }
}
```

-----

### 3.2 휴대폰 인증 코드 확인

**`POST /api/notification/phone/verify`**

발송된 인증 코드를 확인하여 휴대폰 번호를 인증합니다.

### 📊 응답 코드 매트릭스

| 시나리오 | HTTP 상태 | 에러 코드 | 응답 메시지 |
|---|---|---|---|
| ✅ 인증 성공 | `200 OK` | - | "휴대폰 인증이 완료되었습니다" |
| ❌ 인증 필요 | `401 Unauthorized` | AUTH_003 | "인증이 필요합니다" |
| ❌ 잘못된 코드 | `400 Bad Request` | SMS_005 | "인증 코드가 올바르지 않습니다" |
| ❌ 만료된 코드 | `410 Gone` | SMS_006 | "인증 코드가 만료되었습니다" |
| ❌ 인증 세션 없음 | `404 Not Found` | SMS_007 | "인증 세션을 찾을 수 없습니다" |
| ❌ 시도 횟수 초과 | `429 Too Many Requests` | SMS_008 | "인증 시도 횟수를 초과했습니다" |

### Authentication (Required)

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Request Body

| 필드명 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `verificationId` | string | ✓ | 인증 세션 ID |
| `verificationCode` | string | ✓ | 6자리 인증 코드 |

```json
{
  "verificationId": "verify_123456789",
  "verificationCode": "123456"
}
```

### Response (200 OK)

| 필드명 | 타입 | 설명 |
|---|---|---|
| `success` | string | 요청 처리 결과 ("SUCCESS" 또는 "ERROR") |
| `message` | string | 처리 결과 메시지 |
| `data.phoneNumber` | string | 인증된 휴대폰 번호 (마스킹 처리) |
| `data.verifiedAt` | string | 인증 완료 시간 (ISO 8601) |

```json
{
  "success": "SUCCESS",
  "message": "휴대폰 인증이 완료되었습니다",
  "data": {
    "phoneNumber": "010-****-5678",
    "verifiedAt": "2024-01-16T10:32:00Z"
  }
}
```

-----

### 3.3 통합 휴대폰 번호 등록 및 자동 알림 활성화 (🆕 v5 핵심 기능)

**`POST /api/notification/phone/register`**

인증된 휴대폰 번호를 사용자 계정에 등록하고, **자동으로 SMS/이메일 통합 알림을 활성화하며 기존 북마크들의 SMS/이메일 알림도 자동 활성화**합니다.

### 🚀 v5 혁신 기능

  - ✅ **자동 전체 SMS/이메일 활성화**: `user_settings.sms_notification_enabled = TRUE`, `user_settings.email_notification_enabled = TRUE`
  - ✅ **기존 북마크 자동 활성화**: 모든 기존 북마크의 `sms_notification_enabled = TRUE`, `email_notification_enabled = TRUE`
  - ✅ **활성화 결과 알림**: 활성화된 북마크 수 응답에 포함
  - ✅ **사용자 동의 불필요**: 휴대폰 인증 시도 = 알림 받으려는 의도로 간주

### 📊 응답 코드 매트릭스

| 시나리오 | HTTP 상태 | 에러 코드 | 응답 메시지 |
|---|---|---|---|
| ✅ 등록 성공 | `201 Created` | - | "휴대폰 번호가 등록되고 알림이 활성화되었습니다" |
| ❌ 인증 필요 | `401 Unauthorized` | AUTH_003 | "인증이 필요합니다" |
| ❌ 미인증 번호 | `400 Bad Request` | SMS_009 | "인증되지 않은 휴대폰 번호입니다" |
| ❌ 다른 사용자 사용 중 | `409 Conflict` | SMS_010 | "다른 사용자가 사용 중인 번호입니다" |
| ❌ 이미 등록된 번호 | `409 Conflict` | SMS_011 | "이미 등록된 휴대폰 번호입니다" |

### Authentication (Required)

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Request Body

| 필드명 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `verificationId` | string | ✓ | 인증 세션 ID |

```json
{
  "verificationId": "verify_123456789"
}
```

### Response (201 Created)

| 필드명 | 타입 | 설명 |
|---|---|---|
| `success` | string | 요청 처리 결과 ("SUCCESS" 또는 "ERROR") |
| `message` | string | 처리 결과 메시지 |
| `data.phoneNumber` | string | 등록된 휴대폰 번호 (마스킹 처리) |
| `data.registeredAt` | string | 등록 완료 시간 (ISO 8601) |
| `data.smsNotificationEnabled` | boolean | 전체 SMS 알림 활성화 상태 (항상 true) |
| `data.emailNotificationEnabled` | boolean | 전체 이메일 알림 활성화 상태 (항상 true) |
| `data.activatedBookmarksCount` | number | 자동 활성화된 북마크 수 |
| `data.autoActivationSummary` | object | 자동 활성화 요약 정보 |

```json
{
  "success": "SUCCESS",
  "message": "휴대폰 번호가 등록되고 알림이 활성화되었습니다",
  "data": {
    "phoneNumber": "010-****-5678",
    "registeredAt": "2024-01-16T10:35:00Z",
    "smsNotificationEnabled": true,
    "emailNotificationEnabled": true,
    "activatedBookmarksCount": 5,
    "autoActivationSummary": {
      "totalBookmarks": 5,
      "smsActivated": 5,
      "emailActivated": 5,
      "previouslyActivatedSms": 0,
      "previouslyActivatedEmail": 3,
      "newlyActivatedSms": 5,
      "newlyActivatedEmail": 2
    }
  }
}
```

### 🔧 v5 내부 처리 로직 (자동 실행)

1.  **휴대폰 번호 등록**: `users.phone_number`, `users.phone_verified = TRUE`
2.  **전체 SMS/이메일 활성화**: `user_settings.sms_notification_enabled = TRUE`, `user_settings.email_notification_enabled = TRUE`
3.  **기존 북마크 활성화**: `UPDATE bookmarks SET sms_notification_enabled = TRUE, email_notification_enabled = TRUE WHERE user_id = ?`
4.  **활성화 통계 생성**: 활성화된 북마크 수 계산 및 응답 포함

-----

### 3.4 통합 알림 설정 관리 (🆕 v5 핵심 기능)

**`GET/PUT /api/notification/settings`**

대시보드에서 SMS/이메일 알림을 통합 관리하는 기능입니다.

### 📊 응답 코드 매트릭스

| 시나리오 | HTTP 상태 | 에러 코드 | 응답 메시지 |
|---|---|---|---|
| ✅ 조회/설정 성공 | `200 OK` | - | "알림 설정이 조회/변경되었습니다" |
| ❌ 인증 필요 | `401 Unauthorized` | AUTH_003 | "인증이 필요합니다" |
| ❌ 휴대폰 미등록 (SMS 설정 시) | `400 Bad Request` | SMS_019 | "SMS 알림은 휴대폰 인증 후 가능합니다" |

### Authentication (Required)

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### GET Response (200 OK)

| 필드명 | 타입 | 설명 |
|---|---|---|
| `success` | string | 요청 처리 결과 ("SUCCESS" 또는 "ERROR") |
| `message` | string | 처리 결과 메시지 |
| `data.globalSettings` | object | 전체 알림 설정 |
| `data.bookmarkSettings` | array | 북마크별 알림 설정 |
| `data.notificationStats` | object | 알림 통계 |

```json
{
  "success": "SUCCESS",
  "message": "알림 설정이 조회되었습니다",
  "data": {
    "globalSettings": {
      "smsNotificationEnabled": true,
      "emailNotificationEnabled": true,
      "notificationFrequency": "DAILY",
      "notificationTime": "09:00:00"
    },
    "bookmarkSettings": [
      {
        "bookmarkId": "bm_001",
        "displayName": "스마트폰 HS Code",
        "type": "HS_CODE",
        "smsNotificationEnabled": true,
        "emailNotificationEnabled": true
      },
      {
        "bookmarkId": "bm_002",
        "displayName": "1월 수입 화물",
        "type": "CARGO",
        "smsNotificationEnabled": false,
        "emailNotificationEnabled": true
      }
    ],
    "notificationStats": {
      "totalBookmarks": 5,
      "smsEnabledBookmarks": 3,
      "emailEnabledBookmarks": 5,
      "dailyNotificationsSent": 2
    }
  }
}
```

### PUT Request Body

| 필드명 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `smsNotificationEnabled` | boolean | - | 전체 SMS 알림 활성화 |
| `emailNotificationEnabled` | boolean | - | 전체 이메일 알림 활성화 |
| `notificationTime` | string | - | 일일 알림 발송 시간 (HH:mm:ss) |

```json
{
  "smsNotificationEnabled": true,
  "emailNotificationEnabled": true,
  "notificationTime": "10:00:00"
}
```

-----

### 3.5 개별 북마크 알림 설정 변경 (🆕 v5 핵심 기능)

**`PUT /api/bookmarks/{id}/notifications`**

특정 북마크의 SMS/이메일 알림 설정을 개별적으로 변경합니다.

### 🚀 v5 핵심 기능

  - ✅ **개별 세밀 제어**: 각 북마크별로 SMS/이메일 알림 ON/OFF 가능
  - ✅ **실시간 반영**: 설정 변경 즉시 UI에 반영
  - ✅ **권한 검증**: 북마크 소유자만 설정 변경 가능
  - ✅ **모니터링 연동**: 알림 설정이 모두 OFF면 모니터링 대상에서 제외

### 📊 응답 코드 매트릭스

| 시나리오 | HTTP 상태 | 에러 코드 | 응답 메시지 |
|---|---|---|---|
| ✅ 변경 성공 | `200 OK` | - | "북마크 알림 설정이 변경되었습니다" |
| ❌ 인증 필요 | `401 Unauthorized` | AUTH_003 | "인증이 필요합니다" |
| ❌ 휴대폰 미등록 | `400 Bad Request` | SMS_019 | "SMS 알림을 사용하려면 휴대폰 인증이 필요합니다" |
| ❌ 북마크 없음 | `404 Not Found` | BOOKMARK_005 | "북마크를 찾을 수 없습니다" |
| ❌ 권한 없음 | `403 Forbidden` | BOOKMARK_006 | "해당 북마크에 대한 권한이 없습니다" |

### Authentication (Required)

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Path Parameters

| 필드명 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `id` | string | ✓ | 북마크 ID (예: bm_001) |

### Request Body

| 필드명 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `smsNotificationEnabled` | boolean | - | SMS 알림 활성화 여부 |
| `emailNotificationEnabled` | boolean | - | 이메일 알림 활성화 여부 |

```json
{
  "smsNotificationEnabled": true,
  "emailNotificationEnabled": false
}
```

### Response (200 OK)

| 필드명 | 타입 | 설명 |
|---|---|---|
| `success` | string | 요청 처리 결과 ("SUCCESS" 또는 "ERROR") |
| `message` | string | 처리 결과 메시지 |
| `data.bookmarkId` | string | 북마크 ID |
| `data.displayName` | string | 북마크 표시명 |
| `data.previousSettings` | object | 이전 알림 설정 상태 |
| `data.currentSettings` | object | 현재 알림 설정 상태 |
| `data.monitoringActive` | boolean | 모니터링 활성화 상태 |
| `data.changedAt` | string | 변경 시간 (ISO 8601) |

```json
{
  "success": "SUCCESS",
  "message": "북마크 알림 설정이 변경되었습니다",
  "data": {
    "bookmarkId": "bm_001",
    "displayName": "스마트폰 HS Code",
    "previousSettings": {
      "smsNotificationEnabled": false,
      "emailNotificationEnabled": true
    },
    "currentSettings": {
      "smsNotificationEnabled": true,
      "emailNotificationEnabled": false
    },
    "monitoringActive": true,
    "changedAt": "2024-01-16T11:20:00Z"
  }
}
```

-----

## 4. 북마크 관리 시스템 v5 🔒 PRIVATE API

> 🔐 인증 필수: 이 섹션의 모든 API는 **`Authorization: Bearer <Access Token>`** 헤더가 필요합니다.
>  
> 🚀 **v5 변경사항**: 채팅 응답에서 북마크 추가 버튼 제거, 상세페이지에서만 북마크 생성 가능

### 4.1 북마크 목록 조회

**`GET /api/bookmarks`**

사용자의 모든 북마크를 조회합니다.

### 📊 응답 코드 매트릭스

| 시나리오 | HTTP 상태 | 에러 코드 | 응답 메시지 |
|---|---|---|---|
| ✅ 조회 성공 | `200 OK` | - | "북마크 목록 조회됨" |
| ❌ 인증 필요 | `401 Unauthorized` | AUTH_003 | "인증이 필요합니다" |

### Authentication (Required)

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Query Parameters

| 필드명 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `page` | number | - | 페이지 번호 (기본값: 1) |
| `size` | number | - | 페이지 크기 (기본값: 20, 최대 100) |
| `type` | string | - | 북마크 타입 필터 (`HS_CODE`, `CARGO`) |
| `smsEnabled` | boolean | - | SMS 알림 활성화 상태 필터 |
| `emailEnabled` | boolean | - | 이메일 알림 활성화 상태 필터 |
| `sort` | string | - | 정렬 기준 (`createdAt`, `updatedAt`, `displayName`) |
| `order` | string | - | 정렬 순서 (`asc`, `desc`) |

### Response (200 OK)

| 필드명 | 타입 | 설명 |
|---|---|---|
| `success` | string | 요청 처리 결과 ("SUCCESS" 또는 "ERROR") |
| `message` | string | 처리 결과 메시지 |
| `data.bookmarks` | array | 북마크 목록 |
| `data.pagination` | object | 페이징 정보 |
| `data.summary` | object | 북마크 통계 요약 |

```json
{
  "success": "SUCCESS",
  "message": "북마크 목록 조회됨",
  "data": {
    "bookmarks": [
      {
        "id": "bm_001",
        "type": "HS_CODE",
        "targetValue": "8517.12.00",
        "displayName": "스마트폰 HS Code",
        "description": "아이폰 15 프로 수입용",
        "smsNotificationEnabled": true,
        "emailNotificationEnabled": true,
        "monitoringActive": true,
        "createdAt": "2024-01-15T09:30:00Z",
        "updatedAt": "2024-01-16T10:35:00Z"
      },
      {
        "id": "bm_002",
        "type": "CARGO",
        "targetValue": "KRPU1234567890",
        "displayName": "1월 수입 화물",
        "description": "전자제품 수입 화물 추적용",
        "smsNotificationEnabled": false,
        "emailNotificationEnabled": true,
        "monitoringActive": true,
        "createdAt": "2024-01-16T08:20:00Z",
        "updatedAt": "2024-01-16T08:20:00Z"
      }
    ],
    "pagination": {
      "currentPage": 1,
      "totalPages": 1,
      "totalElements": 2,
      "pageSize": 20,
      "hasNext": false,
      "hasPrevious": false
    },
    "summary": {
      "totalBookmarks": 2,
      "hsCodeBookmarks": 1,
      "cargoBookmarks": 1,
      "smsEnabledBookmarks": 1,
      "emailEnabledBookmarks": 2,
      "monitoringActiveBookmarks": 2
    }
  }
}
```

-----

### 4.2 북마크 생성 (v5 상세페이지에서만)

**`POST /api/bookmarks`**

새로운 북마크를 생성합니다. (v5: 상세페이지에서만 가능)

### 📊 응답 코드 매트릭스

| 시나리오 | HTTP 상태 | 에러 코드 | 응답 메시지 |
|---|---|---|---|
| ✅ 생성 성공 | `201 Created` | - | "북마크가 생성되었습니다" |
| ❌ 인증 필요 | `401 Unauthorized` | AUTH_003 | "인증이 필요합니다" |
| ❌ 중복 북마크 | `409 Conflict` | BOOKMARK_001 | "이미 존재하는 북마크입니다" |
| ❌ 잘못된 입력 | `400 Bad Request` | BOOKMARK_002 | "북마크 정보가 올바르지 않습니다" |
| ❌ 북마크 한도 초과 | `429 Too Many Requests` | BOOKMARK_003 | "북마크 개수 한도를 초과했습니다" |
| ❌ 유효하지 않은 HS Code | `422 Unprocessable Entity` | BOOKMARK_004 | "유효하지 않은 HS Code입니다" |

### Authentication (Required)

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Request Body

| 필드명 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `type` | string | ✓ | 북마크 타입 (`HS_CODE`, `CARGO`) |
| `targetValue` | string | ✓ | HS Code 또는 화물관리번호 |
| `displayName` | string | ✓ | 북마크 표시명 |
| `description` | string | - | 북마크 설명 |
| `smsNotificationEnabled` | boolean | - | SMS 알림 활성화 (기본값: 휴대폰 인증 상태에 따라) |
| `emailNotificationEnabled` | boolean | - | 이메일 알림 활성화 (기본값: true) |

```json
{
  "type": "HS_CODE",
  "targetValue": "8517.12.00",
  "displayName": "스마트폰 HS Code",
  "description": "아이폰 15 프로 수입용",
  "smsNotificationEnabled": true,
  "emailNotificationEnabled": true
}
```

### Response (201 Created)

| 필드명 | 타입 | 설명 |
|---|---|---|
| `success` | string | 요청 처리 결과 ("SUCCESS" 또는 "ERROR") |
| `message` | string | 처리 결과 메시지 |
| `data.bookmark` | object | 생성된 북마크 정보 |
| `data.smsSetupRequired` | boolean | SMS 설정이 필요한지 여부 |
| `data.monitoringAutoEnabled` | boolean | v5: 모니터링 자동 활성화 여부 |

```json
{
  "success": "SUCCESS",
  "message": "북마크가 생성되었습니다",
  "data": {
    "bookmark": {
      "id": "bm_003",
      "type": "HS_CODE",
      "targetValue": "8517.12.00",
      "displayName": "스마트폰 HS Code",
      "description": "아이폰 15 프로 수입용",
      "smsNotificationEnabled": true,
      "emailNotificationEnabled": true,
      "monitoringActive": true,
      "createdAt": "2024-01-16T11:00:00Z",
      "updatedAt": "2024-01-16T11:00:00Z"
    },
    "smsSetupRequired": false,
    "monitoringAutoEnabled": true
  }
}
```

-----

### 4.3 북마크 삭제

**`DELETE /api/bookmarks/{id}`**

북마크를 삭제합니다.

### 📊 응답 코드 매트릭스

| 시나리오 | HTTP 상태 | 에러 코드 | 응답 메시지 |
|---|---|---|---|
| ✅ 삭제 성공 | `204 No Content` | - | 응답 본문 없음 |
| ❌ 인증 필요 | `401 Unauthorized` | AUTH_003 | "인증이 필요합니다" |
| ❌ 북마크 없음 | `404 Not Found` | BOOKMARK_005 | "북마크를 찾을 수 없습니다" |
| ❌ 권한 없음 | `403 Forbidden` | BOOKMARK_006 | "해당 북마크에 대한 권한이 없습니다" |

### Authentication (Required)

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Path Parameters

| 필드명 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `id` | string | ✓ | 북마크 ID (예: bm_001) |

### Response (204 No Content)

삭제 성공 시 응답 본문이 없습니다.

-----

## 5. 통합 채팅 시스템 v5 + RAG 🌐 PUBLIC API

> 🌐 공개 API: 이 섹션의 API는 인증 없이 사용 가능합니다.
>  
> 🚀 **v5 혁신**: PostgreSQL+pgvector RAG 시스템 + 병렬 처리 + 상세페이지 버튼 자동 생성

### 5.1 통합 채팅 요청 + RAG 시스템 (🆕 v5 핵심 기능)

**`POST /api/chat`**

사용자의 자연어 질문을 Claude AI + RAG 시스템이 분석하여 무역 관련 의도를 파악하고, 즉시 Server-Sent Events를 통해 분석 과정과 최종 답변을 실시간으로 스트리밍합니다. 동시에 상세페이지 정보를 병렬로 준비합니다.

### 🚀 v5 혁신 기능

- ✅ **RAG 기반 정확도 향상**: PostgreSQL+pgvector로 HSCode 의미적 검색
- ✅ **병렬 처리 시스템**: [자연어 응답 스트리밍] + [상세페이지 정보 준비]
- ✅ **상세페이지 버튼 자동 생성**: 사용자 의도에 맞는 우선순위로 버튼 배치
- ✅ **로딩 최적화**: 상세페이지 버튼 준비 전까지 로딩 스피너 표시
- ✅ **캐시 시스템**: HSCode 벡터 검색 결과를 캐시하여 성능 향상

### 📊 응답 코드 매트릭스

| 시나리오 | HTTP 상태 | 에러 코드 | 응답 메시지 |
|---|---|---|---|
| ✅ 스트리밍 시작 | `200 OK` | - | SSE 스트리밍 시작 |
| ❌ 메시지 너무 짧음 | `400 Bad Request` | CHAT_001 | "메시지는 2자 이상이어야 합니다" |
| ❌ 메시지 너무 김 | `400 Bad Request` | CHAT_002 | "메시지는 1000자 이하여야 합니다" |
| ❌ 무역 외 질의 | `422 Unprocessable Entity` | CHAT_003 | "무역 관련 질문에만 답변할 수 있습니다" |
| ❌ Claude AI 분석 실패 | `502 Bad Gateway` | CHAT_004 | "AI 분석 중 오류가 발생했습니다" |
| ❌ Rate Limit 초과 | `429 Too Many Requests` | RATE_LIMIT_002 | "채팅 요청 한도를 초과했습니다" |
| ❌ RAG 검색 실패 | `502 Bad Gateway` | CHAT_005 | "지식베이스 검색 중 오류가 발생했습니다" |
| ❌ 병렬 처리 실패 | `502 Bad Gateway` | CHAT_006 | "상세페이지 정보 준비 중 오류가 발생했습니다" |

### Request Headers

```
Content-Type: application/json
Accept: text/event-stream
Cache-Control: no-cache
```

### Request Body

| 필드명 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `message` | string | ✓ | 사용자의 자연어 질문 (2~1000자) |
| `context` | object | - | 추가 컨텍스트 정보 (IP, User-Agent 등) |

```json
{
  "message": "아이폰 15 프로를 수입할 때 HS Code와 관세율이 어떻게 되나요?",
  "context": {
    "userAgent": "Mozilla/5.0...",
    "language": "ko"
  }
}
```

### Response (200 OK) - Server-Sent Events

### 🧠 초기 메타데이터 이벤트

```
event: initial_metadata
data: {"claudeIntent": "HS_CODE_ANALYSIS", "estimatedTime": 15, "sessionId": "chat_20240116_123456", "ragEnabled": true, "parallelProcessing": true}

event: thinking_intent_analysis
data: {"stage": "intent_analysis", "content": "사용자 질문을 분석 중입니다. 아이폰 15 프로 수입에 대한 HS Code와 관세율 문의로 판단됩니다.", "progress": 10}
```

### 🔍 Thinking 단계 이벤트 (v5 RAG 추가)

```
event: thinking_rag_search_planning
data: {"stage": "rag_search_planning", "content": "HSCode 벡터 데이터베이스에서 관련 정보를 검색합니다.", "progress": 20}

event: thinking_rag_search_executing
data: {"stage": "rag_search_executing", "content": "스마트폰 관련 HSCode 8517.12.00 정보를 벡터 검색으로 찾았습니다.", "progress": 35}

event: thinking_web_search_executing
data: {"stage": "web_search_executing", "content": "최신 관세율 정보를 웹에서 확인 중입니다.", "progress": 50}

event: thinking_data_processing
data: {"stage": "data_processing", "content": "RAG 검색 결과와 웹 검색 결과를 통합 분석 중입니다.", "progress": 70}

event: thinking_detail_page_preparation
data: {"stage": "detail_page_preparation", "content": "상세페이지 정보를 병렬로 준비 중입니다.", "progress": 85}

event: thinking_response_generation
data: {"stage": "response_generation", "content": "최종 답변을 자연어로 구성하고 관련 정보를 정리합니다.", "progress": 95}
```

### 📝 Main Message 단계 이벤트

```
event: main_message_start
data: {"type": "start", "timestamp": "2024-01-16T10:32:00Z"}

event: main_message_data
data: {"type": "content", "content": "아이폰 15 프로의 정확한 HS Code는 **8517.12.00**입니다.\n\n## 관세율 정보\n- 기본 관세율: 8%\n- FTA 적용 시: 0% (한-미 FTA)\n- 부가가치세: 10%"}

event: main_message_data
data: {"type": "content", "content": "\n\n## 수입 시 주의사항\n1. KC 인증 필수\n2. 전파인증 필요\n3. 개인정보보호 관련 신고"}

event: main_message_complete
data: {"type": "metadata", "sources": [{"title": "관세청 관세율표", "url": "https://unipass.customs.go.kr"}], "relatedInfo": {"hsCode": "8517.12.00", "category": "전자기기"}, "processingTime": 18, "sessionId": "chat_20240116_123456", "ragSources": ["HSCode 벡터 DB", "웹검색"], "cacheHit": false}
```

### 🎯 v5 상세페이지 버튼 이벤트 (병렬 처리)

```
event: detail_page_buttons_start
data: {"type": "start", "timestamp": "2024-01-16T10:32:15Z", "buttonsCount": 3}

event: detail_page_button_ready
data: {"type": "button", "buttonType": "HS_CODE", "priority": 1, "url": "/detail/hscode/8517.12.00", "title": "HS Code 상세정보", "description": "관세율, 규제정보 등", "isReady": true}

event: detail_page_button_ready
data: {"type": "button", "buttonType": "REGULATION", "priority": 2, "url": "/detail/regulation/8517.12.00", "title": "수입 규제정보", "description": "KC인증, 전파인증 등", "isReady": true}

event: detail_page_button_ready
data: {"type": "button", "buttonType": "STATISTICS", "priority": 3, "url": "/detail/statistics/8517.12.00", "title": "무역통계", "description": "수입량, 수입액 통계", "isReady": true}

event: detail_page_buttons_complete
data: {"type": "complete", "timestamp": "2024-01-16T10:32:20Z", "totalPreparationTime": 5}
```

### 🎯 v5 스트리밍 이벤트 타입

### 초기 메타데이터

| 이벤트 타입 | 설명 | 데이터 형식 |
|---|---|---|
| `initial_metadata` | Claude 의도 분석 + RAG 활성화 여부 | `{"claudeIntent": "...", "estimatedTime": 15, "sessionId": "...", "ragEnabled": true, "parallelProcessing": true}` |

### Thinking 단계 (v5 RAG 추가)

| 이벤트 타입 | 설명 | 진행률 |
|---|---|---|
| `thinking_intent_analysis` | 질문 의도 분석 중 | 5-15% |
| `thinking_rag_search_planning` | RAG 검색 계획 수립 중 | 15-25% |
| `thinking_rag_search_executing` | 벡터 DB 검색 실행 중 | 25-40% |
| `thinking_web_search_executing` | 웹검색 실행 중 | 40-60% |
| `thinking_data_processing` | RAG + 웹 데이터 통합 분석 중 | 60-80% |
| `thinking_detail_page_preparation` | 상세페이지 정보 병렬 준비 중 | 80-90% |
| `thinking_response_generation` | 최종 응답 생성 중 | 90-95% |

### Main Message 단계

| 이벤트 타입 | 설명 | 데이터 형식 |
|---|---|---|
| `main_message_start` | 메인 메시지 시작 | `{"type": "start", "timestamp": "..."}` |
| `main_message_data` | 답변 내용 부분별 전송 | `{"type": "content", "content": "..."}` |
| `main_message_complete` | 완료 메타데이터 전송 | `{"type": "metadata", "sources": [...], "ragSources": [...], "cacheHit": false}` |

### v5 상세페이지 버튼 단계 (병렬 처리)

| 이벤트 타입 | 설명 | 데이터 형식 |
|---|---|---|
| `detail_page_buttons_start` | 상세페이지 버튼 준비 시작 | `{"type": "start", "buttonsCount": 3}` |
| `detail_page_button_ready` | 개별 버튼 준비 완료 | `{"type": "button", "buttonType": "HS_CODE", "priority": 1, "url": "...", "isReady": true}` |
| `detail_page_buttons_complete` | 모든 버튼 준비 완료 | `{"type": "complete", "totalPreparationTime": 5}` |

### 🤖 v5 Claude AI + RAG 의도 분석 결과

| 의도 코드 | 설명 | RAG 검색 | 예상 처리 시간 |
|---|---|---|---|
| `HS_CODE_ANALYSIS` | HS Code 분류 및 관세율 조회 | HSCode 벡터 검색 | 12-18초 |
| `CARGO_TRACKING` | 화물 추적 및 상태 조회 | 화물정보 캐시 | 8-12초 |
| `TRADE_REGULATION` | 무역 규제 및 요건 조회 | 규제정보 벡터 검색 | 15-20초 |
| `GENERAL_TRADE_INFO` | 일반 무역 정보 및 절차 | 통합 지식베이스 | 10-15초 |
| `MARKET_ANALYSIS` | 시장 분석 및 통계 | 통계 데이터 검색 | 18-25초 |

### 🔧 v5 클라이언트 연동 예시 (병렬 처리 지원)

```tsx
const startChatWithParallelProcessing = async (message: string) => {
  const response = await fetch("/api/chat", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "text/event-stream",
      "Cache-Control": "no-cache",
    },
    body: JSON.stringify({ message }),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message);
  }

  const reader = response.body?.getReader();
  if (!reader) throw new Error("스트리밍 연결 실패");

  // v5: 병렬 처리 상태 관리
  const processingState = {
    mainMessageComplete: false,
    detailButtonsReady: [],
    allButtonsReady: false
  };

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    const chunk = new TextDecoder().decode(value);
    const lines = chunk.split("\n");

    for (const line of lines) {
      if (line.startsWith("event:")) {
        const eventType = line.slice(6).trim();
      } else if (line.startsWith("data:")) {
        const data = JSON.parse(line.slice(5).trim());
        handleV5StreamEvent(eventType, data, processingState);
      }
    }
  }
};

const handleV5StreamEvent = (eventType: string, data: any, state: any) => {
  switch (eventType) {
    case "initial_metadata":
      showInitialMetadata(data.claudeIntent, data.estimatedTime, data.ragEnabled);
      break;
    
    case "thinking_rag_search_executing":
      updateThinkingUI("🔍 지식베이스 검색", data.content, data.progress);
      break;
    
    case "thinking_detail_page_preparation":
      updateThinkingUI("📋 상세페이지 준비", data.content, data.progress);
      showDetailButtonLoaders(); // 로딩 스피너 표시
      break;
    
    case "main_message_data":
      appendToMainMessage(data.content);
      break;
    
    case "main_message_complete":
      state.mainMessageComplete = true;
      showCompletionInfo(data.sources, data.ragSources, data.cacheHit);
      break;
    
    case "detail_page_button_ready":
      // 버튼이 준비되는 대로 로딩 스피너 → 실제 버튼으로 교체
      replaceButtonLoader(data.buttonType, {
        url: data.url,
        title: data.title,
        description: data.description,
        priority: data.priority
      });
      state.detailButtonsReady.push(data.buttonType);
      break;
    
    case "detail_page_buttons_complete":
      state.allButtonsReady = true;
      hideAllLoaders();
      sortButtonsByPriority(); // 우선순위에 따라 버튼 정렬
      break;
  }
};
```

---

-----

## 6. 대시보드 및 피드 시스템 v5 🔒 PRIVATE API

> 🔐 인증 필수: 이 섹션의 모든 API는 **`Authorization: Bearer <Access Token>`** 헤더가 필요합니다.
>  
> 🚀 **v5 변경사항**: 일일 알림 시스템 반영, 통합 알림 관리 기능 추가

### 6.1 대시보드 요약 정보 조회

**`GET /api/dashboard/summary`**

사용자의 개인화된 대시보드 요약 정보를 조회합니다.

### 📊 응답 코드 매트릭스

| 시나리오 | HTTP 상태 | 에러 코드 | 응답 메시지 |
|---|---|---|---|
| ✅ 조회 성공 | `200 OK` | - | "대시보드 요약 정보 조회됨" |
| ❌ 인증 필요 | `401 Unauthorized` | AUTH_003 | "인증이 필요합니다" |

### Authentication (Required)

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Query Parameters

| 필드명 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `page` | number | - | 페이지 번호 (기본값: 1) |
| `size` | number | - | 페이지 크기 (기본값: 20, 최대 100) |
| `feedType` | string | - | 피드 타입 필터 |
| `importance` | string | - | 중요도 필터 (`HIGH`, `MEDIUM`, `LOW`) |
| `unreadOnly` | boolean | - | 읽지 않은 피드만 조회 (기본값: false) |
| `startDate` | string | - | 조회 시작일 (ISO 8601) |
| `endDate` | string | - | 조회 종료일 (ISO 8601) |
| `includedInDailyNotification` | boolean | - | v5: 일일 알림 포함된 피드만 조회 |

### Response (200 OK)

| 필드명 | 타입 | 설명 |
|---|---|---|
| `success` | string | 요청 처리 결과 ("SUCCESS" 또는 "ERROR") |
| `message` | string | 처리 결과 메시지 |
| `data.feeds` | array | 업데이트 피드 목록 |
| `data.pagination` | object | 페이징 정보 |
| `data.summary` | object | 피드 통계 요약 |

```json
{
  "success": "SUCCESS",
  "message": "업데이트 피드 조회됨",
  "data": {
    "feeds": [
      {
        "id": "feed_001",
        "feedType": "HS_CODE_TARIFF_CHANGE",
        "targetType": "HS_CODE",
        "targetValue": "8517.12.00",
        "title": "스마트폰 관세율 변경 알림",
        "content": "HS Code 8517.12.00의 기본 관세율이 8%에서 6%로 인하되었습니다.",
        "sourceUrl": "https://unipass.customs.go.kr/...",
        "importance": "HIGH",
        "isRead": false,
        "includedInDailyNotification": true,
        "dailyNotificationSentAt": "2024-01-16T09:00:00Z",
        "createdAt": "2024-01-16T08:15:00Z",
        "bookmarkInfo": {
          "bookmarkId": "bm_001",
          "displayName": "스마트폰 HS Code"
        }
      },
      {
        "id": "feed_002",
        "feedType": "CARGO_STATUS_UPDATE",
        "targetType": "CARGO",
        "targetValue": "KRPU1234567890",
        "title": "화물 상태 업데이트",
        "content": "수입신고가 완료되어 통관 절차가 진행 중입니다.",
        "sourceUrl": null,
        "importance": "MEDIUM",
        "isRead": true,
        "includedInDailyNotification": false,
        "dailyNotificationSentAt": null,
        "createdAt": "2024-01-15T16:30:00Z",
        "bookmarkInfo": {
          "bookmarkId": "bm_002",
          "displayName": "1월 수입 화물"
        }
      }
    ],
    "pagination": {
      "currentPage": 1,
      "totalPages": 2,
      "totalElements": 25,
      "pageSize": 20,
      "hasNext": true,
      "hasPrevious": false
    },
    "summary": {
      "totalFeeds": 25,
      "unreadFeeds": 3,
      "highImportanceFeeds": 2,
      "todayFeeds": 5,
      "dailyNotificationFeeds": 12
    }
  }
}
```

---

### 6.3 개별 피드 읽음 처리

**`PUT /api/dashboard/feeds/{feedId}/read`**

특정 피드를 읽음 상태로 표시합니다.

### 📊 응답 코드 매트릭스

| 시나리오 | HTTP 상태 | 에러 코드 | 응답 메시지 |
|---|---|---|---|
| ✅ 처리 성공 | `200 OK` | - | "피드가 읽음 처리되었습니다" |
| ❌ 인증 필요 | `401 Unauthorized` | AUTH_003 | "인증이 필요합니다" |
| ❌ 피드 없음 | `404 Not Found` | FEED_001 | "피드를 찾을 수 없습니다" |
| ❌ 권한 없음 | `403 Forbidden` | FEED_002 | "해당 피드에 대한 권한이 없습니다" |

### Authentication (Required)

```
Cookie: token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Path Parameters

| 필드명 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `feedId` | string | ✓ | 피드 ID (예: feed_001) |

### Response (200 OK)

```json
{
  "success": "SUCCESS",
  "message": "피드가 읽음 처리되었습니다",
  "data": {
    "feedId": "feed_001",
    "isRead": true,
    "readAt": "2024-01-16T11:30:00Z"
  }
}
```

---

### 6.4 전체 피드 읽음 처리

**`PUT /api/dashboard/feeds/read-all`**

사용자의 모든 읽지 않은 피드를 읽음 상태로 일괄 처리합니다.

### 📊 응답 코드 매트릭스

| 시나리오 | HTTP 상태 | 에러 코드 | 응답 메시지 |
|---|---|---|---|
| ✅ 처리 성공 | `200 OK` | - | "모든 피드가 읽음 처리되었습니다" |
| ❌ 인증 필요 | `401 Unauthorized` | AUTH_003 | "인증이 필요합니다" |
| ❌ 읽지 않은 피드 없음 | `404 Not Found` | FEED_003 | "읽지 않은 피드가 없습니다" |

### Authentication (Required)

```
Cookie: token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Request Body (Optional)

| 필드명 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `beforeDate` | string | - | 특정 날짜 이전 피드만 읽음 처리 (ISO 8601) |
| `feedTypes` | array | - | 특정 피드 타입만 읽음 처리 |

```json
{
  "beforeDate": "2024-01-16T00:00:00Z",
  "feedTypes": ["HS_CODE_TARIFF_CHANGE", "CARGO_STATUS_UPDATE"]
}
```

### Response (200 OK)

```json
{
  "success": "SUCCESS",
  "message": "모든 피드가 읽음 처리되었습니다",
  "data": {
    "processedCount": 15,
    "totalUnreadBefore": 15,
    "totalUnreadAfter": 0,
    "processedAt": "2024-01-16T11:35:00Z"
  }
}
```

---

## 7. HSCode RAG 시스템 v5 🌐 PUBLIC API

> 🌐 공개 API: PostgreSQL+pgvector 기반 의미적 검색
> 
> 🚀 **v5 신규**: HSCode 벡터 검색으로 정확도 대폭 향상

### 7.1 HSCode 벡터 검색 (🆕 v5 신규 기능)

**`POST /api/hscode/vector-search`**

자연어 질의를 벡터화하여 PostgreSQL+pgvector에서 의미적으로 유사한 HSCode를 검색합니다.

### 📊 응답 코드 매트릭스

| 시나리오 | HTTP 상태 | 에러 코드 | 응답 메시지 |
|---|---|---|---|
| ✅ 검색 성공 | `200 OK` | - | "HSCode 검색이 완료되었습니다" |
| ❌ 검색어 없음 | `400 Bad Request` | HSCODE_001 | "검색어가 필요합니다" |
| ❌ 벡터 DB 오류 | `502 Bad Gateway` | HSCODE_002 | "벡터 검색 중 오류가 발생했습니다" |
| ❌ 검색 결과 없음 | `404 Not Found` | HSCODE_003 | "관련 HSCode를 찾을 수 없습니다" |

### Request Body

| 필드명 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `query` | string | ✓ | 자연어 검색 질의 |
| `maxResults` | number | - | 최대 결과 수 (기본값: 5, 최대 20) |
| `threshold` | number | - | 유사도 임계값 (기본값: 0.7) |

```json
{
  "query": "스마트폰 아이폰 휴대폰",
  "maxResults": 10,
  "threshold": 0.8
}
```

### Response (200 OK)

| 필드명 | 타입 | 설명 |
|---|---|---|
| `success` | string | 요청 처리 결과 ("SUCCESS" 또는 "ERROR") |
| `message` | string | 처리 결과 메시지 |
| `data.results` | array | 검색 결과 목록 |
| `data.searchInfo` | object | 검색 메타정보 |

```json
{
  "success": "SUCCESS",
  "message": "HSCode 검색이 완료되었습니다",
  "data": {
    "results": [
      {
        "hsCode": "8517.12.00",
        "productName": "스마트폰 및 기타 무선전화기",
        "description": "셀룰러 네트워크나 기타 무선 네트워크용 전화기",
        "similarity": 0.92,
        "classificationBasis": "셀룰러 통신 기능을 가진 휴대용 전화기로서 스마트폰의 핵심 기능에 해당",
        "similarHsCodes": [
          {
            "hsCode": "8517.11.00",
            "difference": "유선 전화기 (무선 기능 없음)"
          },
          {
            "hsCode": "8517.13.00", 
            "difference": "기타 무선전화기 (스마트폰 기능 제외)"
          }
        ],
        "confidence": 0.95,
        "verified": true
      },
      {
        "hsCode": "8517.13.00",
        "productName": "기타 무선전화기",
        "description": "셀룰러 네트워크용이 아닌 무선전화기",
        "similarity": 0.78,
        "classificationBasis": "무선 통신 기능은 있으나 스마트폰과 같은 고급 기능은 제외된 일반 무선전화기",
        "similarHsCodes": [
          {
            "hsCode": "8517.12.00",
            "difference": "스마트폰 (인터넷, 앱 등 고급기능 포함)"
          }
        ],
        "confidence": 0.85,
        "verified": true
      }
    ],
    "searchInfo": {
      "queryVector": "vector_embedding_data",
      "searchTime": 234,
      "totalCandidates": 156,
      "cacheHit": false,
      "ragEnabled": true
    }
  }
}
```

---

### 7.2 HSCode 캐시 저장 (🆕 v5 신규 기능)

**`POST /api/hscode/cache`**

Claude AI가 분석한 HSCode 정보를 벡터 DB에 저장합니다.

### 📊 응답 코드 매트릭스

| 시나리오 | HTTP 상태 | 에러 코드 | 응답 메시지 |
|---|---|---|
| ✅ 저장 성공 | `201 Created` | - | "HSCode 정보가 저장되었습니다" |
| ❌ 잘못된 데이터 | `400 Bad Request` | HSCODE_004 | "HSCode 데이터가 올바르지 않습니다" |
| ❌ 중복 데이터 | `409 Conflict` | HSCODE_005 | "이미 존재하는 HSCode입니다" |
| ❌ 벡터화 실패 | `502 Bad Gateway` | HSCODE_006 | "벡터 임베딩 생성에 실패했습니다" |

### Request Body

| 필드명 | 타입 | 필수 | 설명 |
|---|---|---|
| `hsCode` | string | ✓ | HS Code (예: 8517.12.00) |
| `productName` | string | ✓ | 제품명 |
| `description` | string | ✓ | 상세 설명 |
| `classificationBasis` | string | ✓ | 분류 근거 |
| `similarHsCodes` | array | - | 유사 HSCode 및 차이점 |
| `keywords` | array | - | 검색 키워드 |
| `confidence` | number | - | 분류 신뢰도 (0.0-1.0) |

```json
{
  "hsCode": "8517.12.00",
  "productName": "스마트폰 및 기타 무선전화기",
  "description": "셀룰러 네트워크나 기타 무선 네트워크용 전화기로서 인터넷 접속, 애플리케이션 실행 등의 고급 기능을 포함",
  "classificationBasis": "셀룰러 통신 기능을 가진 휴대용 전화기로서 스마트폰의 핵심 기능에 해당하며, 8517.12 항목의 정의에 부합",
  "similarHsCodes": [
    {
      "hsCode": "8517.11.00",
      "difference": "유선 전화기로서 무선 통신 기능이 없음"
    },
    {
      "hsCode": "8517.13.00",
      "difference": "기타 무선전화기로서 스마트폰과 같은 고급 기능이 제외됨"
    }
  ],
  "keywords": ["스마트폰", "아이폰", "휴대폰", "무선전화기", "셀룰러"],
  "confidence": 0.95
}
```

### Response (201 Created)

```json
{
  "success": "SUCCESS",
  "message": "HSCode 정보가 저장되었습니다",
  "data": {
    "hsCode": "8517.12.00",
    "vectorId": "vec_1234567890",
    "embedding": "vector_data_stored",
    "createdAt": "2024-01-16T12:00:00Z",
    "verified": false
  }
}
```

---

## 8. 에러 코드 정의 v5

### 인증 관련 (AUTH_xxx)

| 에러 코드 | HTTP 상태 | 설명 |
|---|---|---|
| AUTH_001 | 401 | 잘못된 인증 정보 |
| AUTH_002 | 423 | 계정 잠김 |
| AUTH_003 | 401 | JWT 토큰 만료 또는 인증 필요 |
| AUTH_004 | 401 | 유효하지 않은 JWT 토큰 |

### SMS/이메일 통합 알림 관련 (SMS_xxx, EMAIL_xxx)

| 에러 코드 | HTTP 상태 | 설명 |
|---|---|---|
| SMS_001 | 400 | 잘못된 휴대폰 번호 형식 |
| SMS_002 | 409 | 이미 인증된 휴대폰 번호 |
| SMS_003 | 429 | 발송 한도 초과 |
| SMS_004 | 502 | SMS 서비스 오류 |
| SMS_005 | 400 | 잘못된 인증 코드 |
| SMS_006 | 410 | 만료된 인증 코드 |
| SMS_007 | 404 | 인증 세션 없음 |
| SMS_008 | 429 | 인증 시도 횟수 초과 |
| SMS_009 | 400 | 미인증 휴대폰 번호 |
| SMS_010 | 409 | 다른 사용자가 사용 중인 번호 |
| SMS_011 | 409 | 이미 등록된 휴대폰 번호 |
| SMS_019 | 400 | 휴대폰 인증 필요 (SMS 설정 시) |
| EMAIL_001 | 404 | 활성화할 북마크 없음 (이메일) |
| EMAIL_002 | 400 | 이메일 인증 필요 |

### 북마크 관련 (BOOKMARK_xxx)

| 에러 코드 | HTTP 상태 | 설명 |
|---|---|---|
| BOOKMARK_001 | 409 | 중복 북마크 |
| BOOKMARK_002 | 400 | 잘못된 북마크 정보 |
| BOOKMARK_003 | 429 | 북마크 개수 한도 초과 |
| BOOKMARK_004 | 422 | 유효하지 않은 HS Code |
| BOOKMARK_005 | 404 | 북마크 없음 |
| BOOKMARK_006 | 403 | 북마크 권한 없음 |
| BOOKMARK_007 | 400 | 잘못된 알림 설정 데이터 |

### 채팅 관련 (CHAT_xxx)

| 에러 코드 | HTTP 상태 | 설명 |
|---|---|---|
| CHAT_001 | 400 | 메시지 너무 짧음 (2자 미만) |
| CHAT_002 | 400 | 메시지 너무 김 (1000자 초과) |
| CHAT_003 | 422 | 무역 관련 질문이 아님 |
| CHAT_004 | 502 | Claude AI 분석 실패 |
| CHAT_005 | 502 | 지식베이스 검색 실패 |
| CHAT_006 | 502 | 상세페이지 정보 준비 실패 |

### HSCode RAG 시스템 관련 (HSCODE_xxx) - v5 신규

| 에러 코드 | HTTP 상태 | 설명 |
|---|---|---|
| HSCODE_001 | 400 | 검색어 필요 |
| HSCODE_002 | 502 | 벡터 검색 오류 |
| HSCODE_003 | 404 | 검색 결과 없음 |
| HSCODE_004 | 400 | 잘못된 HSCode 데이터 |
| HSCODE_005 | 409 | 중복 HSCode |
| HSCODE_006 | 502 | 벡터 임베딩 생성 실패 |

### 피드 관련 (FEED_xxx)

| 에러 코드 | HTTP 상태 | 설명 |
|---|---|---|
| FEED_001 | 404 | 피드 없음 |
| FEED_002 | 403 | 피드 권한 없음 |
| FEED_003 | 404 | 읽지 않은 피드 없음 |

### Rate Limiting (RATE_LIMIT_xxx)

| 에러 코드 | HTTP 상태 | 설명 |
|---|---|---|
| RATE_LIMIT_001 | 429 | 로그인 시도 한도 초과 |
| RATE_LIMIT_002 | 429 | 채팅 요청 한도 초과 |

### 공통 에러 (COMMON_xxx)

| 에러 코드 | HTTP 상태 | 설명 |
|---|---|---|
| COMMON_001 | 400 | 필수 입력 정보 누락 |
| COMMON_002 | 500 | 서버 내부 오류 |

---

## **개발자 가이드 v5 (개선판)**

### **9.1 PostgreSQL + pgvector RAG 시스템 구현**

  * **개념** : RAG(Retrieval-Augmented Generation)는 대규모 언어 모델(LLM)이 외부 지식 베이스를 참조하여 답변의 정확성과 신뢰도를 높이는 기술입니다. 본 시스템에서는 PostgreSQL 데이터베이스와 pgvector 확장을 사용하여 HSCode 관련 정보를 벡터 형태로 저장하고, 사용자의 자연어 질문과 의미적으로 가장 유사한 정보를 검색하여 Claude AI의 답변 생성에 활용합니다.
  * **구현 라이브러리** : `langchain4j-pgvector`는 pgvector 저장소와의 상호작용을, `langchain4j-voyage-ai-spring-boot-starter`는 텍스트 임베딩을 위한 `EmbeddingModel` Bean을 자동으로 설정합니다.
  * **핵심 로직** 은 다음과 같습니다.
    1.  HSCode 정보를 텍스트 형태로 구성하고, VoyageAI 모델을 사용해 1536차원의 벡터로 임베딩합니다.
    2.  생성된 벡터를 원본 텍스트 및 메타데이터와 함께 `PgVectorEmbeddingStore`를 통해 PostgreSQL에 저장합니다.
    3.  사용자 질문이 들어오면, 질문 또한 벡터로 변환하여 데이터베이스에 저장된 벡터들과 코사인 유사도(cosine similarity)를 비교, 가장 관련성 높은 문서를 검색합니다.
    4.  검색된 문서를 컨텍스트로 Claude AI에 전달하여 더 정확한 답변을 생성하도록 합니다.

#### **🔧 Spring Boot 3.5+ 설정**

  * **pom.xml : Maven 의존성 설정**

      * Langchain4j 모듈들의 호환성을 보장하고 버전을 편리하게 관리하기 위해 `langchain4j-bom`을 사용합니다. 이를 통해 각 모듈의 버전을 개별적으로 명시할 필요가 없습니다.

    ```xml
    <properties>
        <langchain4j.version>1.1.0</langchain4j.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>dev.langchain4j</groupId>
                <artifactId>langchain4j-bom</artifactId>
                <version>${langchain4j.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-anthropic-spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-voyage-ai-spring-boot-starter</artifactId>
        </dependency>

        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j</artifactId>
        </dependency>

        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-pgvector</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
    </dependencies>
    ```

  * **application.properties : 외부 설정**

      * 스프링 부트 스타터를 사용함으로써, `VoyageAiEmbeddingModel` Bean을 직접 생성할 필요 없이 외부 설정 파일에서 모든 구성을 관리할 수 있습니다.

    

    ```properties
    # VoyageAI Embedding Model 자동 설정을 위한 프로퍼티
    langchain4j.voyage-ai.embedding-model.api-key=${VOYAGE_API_KEY}
    langchain4j.voyage-ai.embedding-model.model-name=voyage-3-large
    langchain4j.voyage-ai.embedding-model.output-dimension=1536
    langchain4j.voyage-ai.embedding-model.input-type=document
    langchain4j.voyage-ai.embedding-model.timeout=60s
    langchain4j.voyage-ai.embedding-model.max-retries=3
    ```

  * **RagConfig.java : RAG 구성 Bean**

      * `EmbeddingModel` Bean은 스타터가 자동으로 생성하므로 설정 코드에서 삭제합니다. `EmbeddingStore`는 Spring Boot가 관리하는 `DataSource`를 직접 사용하여 DB 커넥션 풀을 효율적으로 재사용하도록 수정합니다.

    

    ```java
    @Configuration
    public class RagConfig {
        
        @Bean
        public EmbeddingStore<TextSegment> embeddingStore(DataSource dataSource) {
            // Spring Boot가 관리하는 DataSource를 사용하여 DB 연결 정보 중복 제거 및 커넥션 풀 재사용
            return PgVectorEmbeddingStore.builder()
                    .dataSource(dataSource)
                    .table("hscode_vectors")
                    .dimension(1536)
                    .createTable(true)
                    .dropTableFirst(false)
                    .build();
        }
        
        // langchain4j-voyage-ai-spring-boot-starter가 EmbeddingModel Bean을 자동 생성하므로,
        // 아래의 수동 설정 코드는 더 이상 필요하지 않음.
    }
    ```

#### **🔍 RAG 기반 HSCode 검색 서비스**

  * `HsCodeRagService.java` 파일의 코드는 수정할 필요가 없습니다. 이미 잘 작성되어 있으며, 변경된 설정과 완벽하게 호환됩니다.

### **9.2 병렬 처리 채팅 시스템 구현**

  * **개념** : 사용자 경험을 최적화하기 위해, AI의 자연어 응답 생성과 상세 페이지에 필요한 데이터 준비 작업을 동시에 병렬로 처리합니다. 이를 통해 사용자는 AI의 답변을 실시간 스트리밍으로 확인하면서, 관련된 상세 정보 페이지로 즉시 이동할 수 있게 됩니다.
  * **구현 방식** : Java의 `CompletableFuture`와 `ExecutorService`를 활용하여 두 개 이상의 비동기 작업을 병렬로 실행합니다.
  * **핵심 로직** 은 다음과 같습니다.
    1.  클라이언트로부터 `/api/chat` 요청이 들어오면, `SseEmitter` 객체를 생성하여 Server-Sent Events 스트리밍 연결을 엽니다.
    2.  `CompletableFuture.runAsync()`를 사용하여 두 개의 비동기 작업을 병렬 실행 큐에 등록합니다.
          * **작업 1 (자연어 응답)** : RAG 시스템을 통해 검색된 정보를 바탕으로 Claude AI가 답변을 생성하고, 이를 `SseEmitter`를 통해 클라이언트에 실시간으로 스트리밍합니다. (`thinking_*`, `main_message_*` 이벤트)
          * **작업 2 (상세페이지 준비)** : 사용자 질문의 의도를 분석하여 필요한 상세 정보(예: HSCode 상세, 규제 정보)를 백그라운드에서 조회 및 캐싱합니다. 각 정보가 준비될 때마다 `detail_page_button_ready` 이벤트를 전송합니다.
    3.  `CompletableFuture.allOf()`를 사용하여 모든 병렬 작업이 완료되면 스트리밍 연결을 정상적으로 종료합니다.

#### **🌊 병렬 처리 ChatController**

  * `ChatController.java`의 코드는 수정할 필요 없이 훌륭한 구조를 가지고 있습니다. 다만, 사용자 경험을 극대화하기 위해 Langchain4j의 `StreamingChatLanguageModel`을 활용하여 **실시간 토큰 단위 스트리밍**을 구현하는 것을 고려할 수 있습니다. 이는 사용자가 첫 글자를 훨씬 빠르게 볼 수 있게 하여 체감 성능을 향상시킵니다.

### **9.3 통합 알림 시스템 구현**

  * **개념** : 사용자가 북마크한 항목의 변동사항을 SMS와 이메일을 통해 통합적으로 알립니다. 기존의 즉시 알림 방식에서, 하루 동안의 변경사항을 요약하여 특정 시간에 발송하는 일일 알림(Daily Digest) 방식으로 변경되었습니다.
  * **구현 방식** : Spring의 `@Scheduled` 어노테이션을 사용하여 매일 특정 시간에 알림 발송 로직이 실행되도록 설정합니다. (예: `cron = "0 0 9 * * *"`은 매일 오전 9시를 의미)
  * **핵심 로직** 은 다음과 같습니다.
    1.  **자동 활성화** : 사용자가 휴대폰 인증을 완료하면, 해당 사용자의 전체 알림 설정을 활성화하고, 기존에 생성했던 모든 북마크의 알림 설정 또한 자동으로 ON 상태로 변경합니다.
    2.  **일일 알림 발송**: 스케줄러가 동작하면, 알림이 활성화된 모든 사용자를 조회합니다.
    3.  각 사용자에 대해, 지난 24시간 동안 발생한 새로운 업데이트 피드(UpdateFeed)를 조회합니다.
    4.  조회된 피드가 있으면, 이를 요약한 알림 메시지를 생성합니다.
    5.  사용자의 설정에 따라 SMS와 이메일 서비스를 통해 요약 메시지를 발송하고, 발송 내역을 로그로 기록합니다.

#### **📧 SMS/이메일 통합 알림 서비스 (개선판)**

  * 대량의 알림 발송 시 발생할 수 있는 **N+1 쿼리 문제**를 방지하기 위해, 루프 내에서 반복적으로 DB를 조회하는 대신 사용자 정보를 한 번만 조회하여 재사용하도록 코드를 수정했습니다.

    ```java
    @Service
    public class IntegratedNotificationService {
        
    private final SmsService smsService;
    private final EmailService emailService;
    private final UserSettingsRepository userSettingsRepository;
    private final BookmarkRepository bookmarkRepository;
    private final NotificationLogRepository notificationLogRepository;
    
    // 휴대폰 인증 완료 시 자동 알림 활성화
    @Transactional
    public void autoActivateNotificationsOnPhoneVerification(Long userId) {
        try {
            // 1. 전체 SMS/이메일 알림 활성화
            UserSettings settings = userSettingsRepository.findByUserId(userId)
                    .orElseThrow(() -> new NotFoundException("사용자 설정을 찾을 수 없습니다"));
            
            settings.setSmsNotificationEnabled(true);
            settings.setEmailNotificationEnabled(true);
            userSettingsRepository.save(settings);
            
            // 2. 모든 기존 북마크 알림 활성화
            List<Bookmark> bookmarks = bookmarkRepository.findByUserId(userId);
            bookmarks.forEach(bookmark -> {
                bookmark.setSmsNotificationEnabled(true);
                bookmark.setEmailNotificationEnabled(true);
            });
            bookmarkRepository.saveAll(bookmarks);
            
            log.info("사용자 {}의 알림이 자동 활성화되었습니다. 북마크 {}개 활성화", 
                    userId, bookmarks.size());
            
        } catch (Exception e) {
            log.error("알림 자동 활성화 실패: userId={}", userId, e);
            throw new NotificationException("알림 자동 활성화에 실패했습니다", e);
        }
    }
    
    // 일일 통합 알림 발송
    @Scheduled(cron = "0 0 9 * * *") // 매일 오전 9시
    public void sendDailyNotifications() {
        log.info("일일 통합 알림 발송 시작");
        
        try {
            // 알림 설정이 활성화된 사용자 조회
            List<UserSettings> activeUsers = userSettingsRepository
                    .findBySmsNotificationEnabledTrueOrEmailNotificationEnabledTrue();
            
            for (UserSettings settings : activeUsers) {
                processDailyNotificationForUser(settings);
            }
            
            log.info("일일 통합 알림 발송 완료: {}명 처리", activeUsers.size());
            
        } catch (Exception e) {
            log.error("일일 알림 발송 중 오류 발생", e);
        }
    }

        private void processDailyNotificationForUser(UserSettings settings) {
            try {
                Long userId = settings.getUserId();
                
                // [개선] N+1 쿼리 방지를 위해 User 정보를 루프 초반에 한 번만 조회
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) {
                    log.warn("알림 발송 대상 사용자 정보를 찾을 수 없습니다: userId={}", userId);
                    return;
                }

                List<UpdateFeed> unreadFeeds = updateFeedRepository
                        .findUnreadFeedsByUserIdAndDateRange(userId, LocalDateTime.now().minusDays(1), LocalDateTime.now());

                if (unreadFeeds.isEmpty()) {
                    return;
                }

                String notificationContent = buildDailyNotificationContent(unreadFeeds);
                String emailSubject = String.format("무역 정보 업데이트 알림 (%d건)", unreadFeeds.size());

                // [개선] 조회된 User 객체 재사용
                if (settings.isSmsNotificationEnabled() && user.getPhoneNumber() != null) {
                    sendSmsNotification(user.getPhoneNumber(), notificationContent, userId);
                }

                // [개선] 조회된 User 객체 재사용
                if (settings.isEmailNotificationEnabled()) {
                    sendEmailNotification(user.getEmail(), emailSubject, notificationContent, userId);
                }
                
                // 피드 일일 알림 처리 완료 표시
            unreadFeeds.forEach(feed -> {
                feed.setIncludedInDailyNotification(true);
                feed.setDailyNotificationSentAt(LocalDateTime.now());
            });
            updateFeedRepository.saveAll(unreadFeeds);
            } catch (Exception e) {
                log.error("사용자 {}의 일일 알림 처리 실패", settings.getUserId(), e);
            }
        }
        
         private void sendSmsNotification(String phoneNumber, String content, Long userId) {
        try {
            String maskedContent = content.length() > 90 
                    ? content.substring(0, 87) + "..." 
                    : content;
            
            SmsResult result = smsService.sendSms(phoneNumber, maskedContent);
            
            // 발송 로그 기록
            NotificationLog log = NotificationLog.builder()
                    .userId(userId)
                    .notificationType(NotificationType.SMS)
                    .messageType(MessageType.DAILY_NOTIFICATION)
                    .recipient(phoneNumber)
                    .content(maskedContent)
                    .status(result.isSuccess() ? NotificationStatus.SENT : NotificationStatus.FAILED)
                    .externalMessageId(result.getMessageId())
                    .costKrw(result.getCost())
                    .sentAt(LocalDateTime.now())
                    .build();
            
            notificationLogRepository.save(log);
            
        } catch (Exception e) {
            log.error("SMS 발송 실패: phoneNumber={}, userId={}", phoneNumber, userId, e);
        }
    }
    
    private void sendEmailNotification(String email, String subject, String content, Long userId) {
        try {
            EmailResult result = emailService.sendEmail(email, subject, content);
            
            // 발송 로그 기록
            NotificationLog log = NotificationLog.builder()
                    .userId(userId)
                    .notificationType(NotificationType.EMAIL)
                    .messageType(MessageType.DAILY_NOTIFICATION)
                    .recipient(email)
                    .title(subject)
                    .content(content)
                    .status(result.isSuccess() ? NotificationStatus.SENT : NotificationStatus.FAILED)
                    .externalMessageId(result.getMessageId())
                    .sentAt(LocalDateTime.now())
                    .build();
            
            notificationLogRepository.save(log);
            
        } catch (Exception e) {
            log.error("이메일 발송 실패: email={}, userId={}", email, userId, e);
        }
    }
    
    private String buildDailyNotificationContent(List<UpdateFeed> feeds) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 오늘의 무역 정보 업데이트\n\n");
        
        for (int i = 0; i < Math.min(feeds.size(), 5); i++) {
            UpdateFeed feed = feeds.get(i);
            sb.append(String.format("%d. %s\n", i + 1, feed.getTitle()));
            
            if (feed.getContent().length() > 50) {
                sb.append(feed.getContent().substring(0, 47)).append("...\n");
            } else {
                sb.append(feed.getContent()).append("\n");
            }
            sb.append("\n");
        }
        
        if (feeds.size() > 5) {
            sb.append(String.format("외 %d건의 업데이트가 더 있습니다.\n", feeds.size() - 5));
        }
        
        sb.append("자세한 내용은 대시보드에서 확인하세요.");
        
        return sb.toString();
    }
}
```

### **9.4 v5 프론트엔드 연동 가이드**

  * **개념** : v5의 핵심 기능인 병렬 처리 스트리밍을 클라이언트에서 올바르게 처리하기 위한 가이드입니다. 클라이언트는 여러 종류의 SSE 이벤트를 비동기적으로 수신하고, 각 이벤트 타입에 맞춰 UI를 동적으로 업데이트해야 합니다.
  * **구현 방식** : `fetch` API를 사용하여 SSE 스트리밍 연결을 수립하고, `ReadableStream`을 통해 들어오는 데이터를 실시간으로 파싱합니다. React 환경에서는 `useState`와 `useEffect`를 사용하여 상태를 관리하고 UI를 렌더링합니다.
  * **핵심 로직** 은 다음과 같습니다.
    1.  **스트림 처리**: 수신된 데이터(`chunk`)를 `TextDecoder`로 디코딩하고, `event:`와 `data:` 필드를 파싱하여 이벤트 타입과 데이터를 분리합니다.
    2.  **병렬 UI 업데이트**:
          * `thinking_*` 이벤트 수신 시 : AI의 사고 과정을 보여주는 UI 영역(로딩 바, 텍스트)을 업데이트합니다.
          * `main_message_data` 이벤트 수신 시 : 메인 답변 영역에 텍스트를 점진적으로 추가합니다.
          * `detail_page_buttons_start` 이벤트 수신 시 : 상세페이지 버튼 영역에 로딩 스피너를 표시합니다.
          * `detail_page_button_ready` 이벤트 수신 시 : 해당 버튼의 로딩 스피너를 실제 버튼 UI로 교체합니다. 이 작업은 각 버튼이 준비되는 순서대로 독립적으로 일어납니다.
    3.  **상태 관리**: `isStreaming`, `currentThinking`, `detailButtons` 등의 상태를 통해 현재 스트리밍 진행 상태를 추적하고, 사용자 입력 비활성화 등 UI를 제어합니다.

#### **🎨 병렬 처리 지원 React 컴포넌트 (개선판)**

  * 스트리밍 안정성을 높이기 위해, `main_message_data`와 같이 단순 텍스트일 수 있는 이벤트를 처리할 때 불필요한 `JSON.parse` 호출로 인해 오류가 발생하는 것을 방지하도록 수정했습니다.

    ```tsx
    import React, { useState, useRef, useCallback, useEffect } from "react";

interface ChatMessage {
  id: string;
  content: string;
  type: "user" | "assistant";
  timestamp: Date;
  thinking?: ThinkingData;
  metadata?: CompletionMetadata;
  detailButtons?: DetailButton[];
}

interface DetailButton {
  type: string;
  priority: number;
  url: string;
  title: string;
  description: string;
  isReady: boolean;
  isLoading: boolean;
}

const V5ChatInterface: React.FC = () => {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [isStreaming, setIsStreaming] = useState(false);
  const [currentThinking, setCurrentThinking] = useState<ThinkingData | null>(null);
  const [detailButtons, setDetailButtons] = useState<DetailButton[]>([]);
  
  const handleSendMessage = useCallback(async () => {
    if (!input.trim() || isStreaming) return;

    const userMessage: ChatMessage = {
      id: Date.now().toString(),
      content: input.trim(),
      type: "user",
      timestamp: new Date(),
    };

    setMessages(prev => [...prev, userMessage]);
    setInput("");
    setIsStreaming(true);
    setCurrentThinking(null);
    setDetailButtons([]);

    const assistantMessage: ChatMessage = {
      id: (Date.now() + 1).toString(),
      content: "",
      type: "assistant",
      timestamp: new Date(),
      detailButtons: []
    };

    setMessages(prev => [...prev, assistantMessage]);

    try {
      const response = await fetch("/api/chat", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Accept: "text/event-stream",
          "Cache-Control": "no-cache",
        },
        body: JSON.stringify({ message: userMessage.content }),
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message);
      }

      await processV5StreamingResponse(response, assistantMessage.id);
    } catch (error) {
      console.error("채팅 시작 실패:", error);
      setIsStreaming(false);
    }
  }, [input, isStreaming]);

  const processV5StreamingResponse = async (response: Response, messageId: string) => {
    const reader = response.body?.getReader();
    if (!reader) throw new Error("스트리밍 연결 실패");

    const decoder = new TextDecoder();
    let buffer = "";

    try {
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split("\n");
        buffer = lines.pop() || "";

        processV5SSELines(lines, messageId);
      }
    } finally {
      reader.releaseLock();
      setIsStreaming(false);
    }
  };

    const handleV5StreamEvent = (eventType: string, data: string, messageId: string) => {
        // [개선] main_message_data 이벤트는 JSON 파싱 없이 바로 텍스트로 처리
        if (eventType === "main_message_data") {
            // 백엔드가 JSON 형식으로 보낸다면 `JSON.parse(data).content` 사용
            // 백엔드가 일반 텍스트로 보낸다면 `data`를 직접 사용
            appendToMessage(messageId, data); 
            return;
        }
        
        try {
            const parsedData = JSON.parse(data);

            switch (eventType) {
                // ... 기존 case 문들은 유지 ...
                // `main_message_data` case는 위에서 처리했으므로 여기서는 제외 가능
                 case "initial_metadata":
          handleInitialMetadata(parsedData);
          break;

        case "thinking_rag_search_executing":
          handleRagThinking(parsedData);
          break;

        case "thinking_detail_page_preparation":
          handleDetailPagePreparation(parsedData);
          break;
        case "main_message_complete":
          handleMainMessageComplete(messageId, parsedData);
          break;

        case "detail_page_buttons_start":
          initializeDetailButtons(parsedData.buttonsCount);
          break;

        case "detail_page_button_ready":
          updateDetailButton(parsedData);
          break;

        case "detail_page_buttons_complete":
          finalizeDetailButtons();
          break;
                
                default:
                    if (eventType.startsWith("thinking_")) {
                        updateThinkingArea(parsedData);
                    }
            }
        } catch (error) {
            console.error("SSE 이벤트 처리 오류:", { eventType, data, error });
       }
  };

  const handleInitialMetadata = (data: any) => {
    console.log("v5 초기 메타데이터:", data.claudeIntent, data.ragEnabled);
  };

  const handleRagThinking = (data: any) => {
    setCurrentThinking({
      stage: "🔍 지식베이스 검색",
      content: data.content,
      progress: data.progress,
    });
  };

  const handleDetailPagePreparation = (data: any) => {
    setCurrentThinking({
      stage: "📋 상세페이지 준비",
      content: data.content,
      progress: data.progress,
    });
  };

  const initializeDetailButtons = (count: number) => {
    const buttons: DetailButton[] = [];
    for (let i = 0; i < count; i++) {
      buttons.push({
        type: `LOADING_${i}`,
        priority: i + 1,
        url: "",
        title: "준비 중...",
        description: "상세 정보를 준비하고 있습니다",
        isReady: false,
        isLoading: true,
      });
    }
    setDetailButtons(buttons);
  };

  const updateDetailButton = (data: any) => {
    setDetailButtons(prev => 
      prev.map(button => 
        button.priority === data.priority
          ? {
              ...button,
              type: data.buttonType,
              url: data.url,
              title: data.title,
              description: data.description,
              isReady: true,
              isLoading: false,
            }
          : button
      )
    );
  };

  const finalizeDetailButtons = () => {
    // 우선순위에 따라 버튼 정렬
    setDetailButtons(prev => 
      [...prev].sort((a, b) => a.priority - b.priority)
    );
    setCurrentThinking(null);
  };

  const appendToMessage = (messageId: string, content: string) => {
    setMessages(prev =>
      prev.map(msg =>
        msg.id === messageId
          ? { ...msg, content: msg.content + content }
          : msg
      )
    );
  };

  return (
    <div className="v5-chat-interface">
      <div className="messages-container">
        {messages.map(message => (
          <div key={message.id} className={`message ${message.type}`}>
            <div className="message-content">{message.content}</div>
            
            {/* v5: 상세페이지 버튼들 */}
            {message.type === "assistant" && detailButtons.length > 0 && (
              <div className="detail-buttons-container">
                {detailButtons.map((button, index) => (
                  <button
                    key={index}
                    className={`detail-button ${button.isLoading ? 'loading' : ''}`}
                    disabled={!button.isReady}
                    onClick={() => button.isReady && window.open(button.url, '_blank')}
                  >
                    {button.isLoading ? (
                      <div className="loading-spinner" />
                    ) : (
                      <>
                        <div className="button-title">{button.title}</div>
                        <div className="button-description">{button.description}</div>
                      </>
                    )}
                  </button>
                ))}
              </div>
            )}
          </div>
        ))}

        {/* v5: Thinking 영역 */}
        {currentThinking && (
          <div className="thinking-area v5">
            <div className="thinking-header">
              <span className="thinking-stage">{currentThinking.stage}</span>
              <div className="progress-bar">
                <div
                  className="progress-fill"
                  style={{ width: `${currentThinking.progress}%` }}
                />
              </div>
            </div>
            <div className="thinking-content">{currentThinking.content}</div>
          </div>
        )}
      </div>

      <div className="input-area">
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyPress={(e) => e.key === "Enter" && handleSendMessage()}
          placeholder="무역 관련 질문을 입력하세요... (v5: RAG 지원)"
          disabled={isStreaming}
          className="chat-input"
        />
        <button
          onClick={handleSendMessage}
          disabled={!input.trim() || isStreaming}
          className="send-button"
        >
          {isStreaming ? "처리 중..." : "전송"}
        </button>
      </div>
    </div>
  );
};

export default V5ChatInterface;
```

### 10.2 애플리케이션 설정 변경

### 📝 application.yml 업데이트

```yaml
# v5 PostgreSQL + pgvector 설정
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/trade_radar_v5
    username: postgres
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
  
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        jdbc:
          batch_size: 25
          batch_versioned_data: true
        order_inserts: true
        order_updates: true

  # v5 Redis 단순화 (임시 데이터 전용)
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    timeout: 2000ms
    database: 0

# v5 RAG 시스템 설정
rag:
  voyage:
    api-key: ${VOYAGE_API_KEY}
    model: voyage-3-large
    dimension: 1536
  
  pgvector:
    table-name: hscode_vectors
    max-results: 10
    similarity-threshold: 0.7

# v5 통합 알림 시스템
notification:
  sms:
    provider: ${SMS_PROVIDER:coolsms}
    api-key: ${SMS_API_KEY}
    api-secret: ${SMS_API_SECRET}
    from-number: ${SMS_FROM_NUMBER}
  
  email:
    provider: ${EMAIL_PROVIDER:smtp}
    host: ${EMAIL_HOST}
    port: ${EMAIL_PORT:587}
    username: ${EMAIL_USERNAME}
    password: ${EMAIL_PASSWORD}
  
  daily:
    enabled: true
    cron: "0 0 9 * * *"  # 매일 오전 9시
    max-feeds-per-notification: 5

# v5.1 Access/Refresh 토큰 인증
jwt:
  secret: ${JWT_SECRET}
  access-token-expiration: 3600000  # 1시간
  refresh-token-expiration: 1209600000  # 14일
  
# v5 병렬 처리 설정
async:
  executor:
    core-pool-size: 10
    max-pool-size: 50
    queue-capacity: 100
```
## 11. 성능 최적화 가이드 v5

### 11.1 PostgreSQL + pgvector 최적화

### 🔧 데이터베이스 튜닝

```sql
-- PostgreSQL 성능 최적화 설정
ALTER SYSTEM SET shared_buffers = '256MB';
ALTER SYSTEM SET effective_cache_size = '1GB';
ALTER SYSTEM SET maintenance_work_mem = '64MB';
ALTER SYSTEM SET checkpoint_completion_target = 0.9;
ALTER SYSTEM SET wal_buffers = '16MB';
ALTER SYSTEM SET default_statistics_target = 100;

-- pgvector 인덱스 최적화
CREATE INDEX CONCURRENTLY idx_hscode_vectors_embedding_optimized 
ON hscode_vectors USING hnsw (embedding vector_cosine_ops) 
WITH (m = 32, ef_construction = 128);

-- 정기적인 통계 업데이트
ANALYZE hscode_vectors;
```

### 11.2 Spring Boot 3.5+ 성능 최적화

```yaml
# v5 성능 최적화 설정
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50
          fetch_size: 100
        cache:
          use_second_level_cache: true
          use_query_cache: true
        order_inserts: true
        order_updates: true
  
  task:
    execution:
      pool:
        core-size: 20
        max-size: 100
        queue-capacity: 200
        keep-alive: 60s

# JVM 튜닝
server:
  tomcat:
    threads:
      max: 300
      min-spare: 50
    connection-timeout: 60000
    max-connections: 8192
```

---

**🎉 AI 기반 무역 규제 레이더 플랫폼 API 명세서 v5 완료!**

이제 PostgreSQL+pgvector RAG 시스템과 SMS/이메일 통합 알림, 병렬 처리 등 혁신적인 기능들로 중소기업 사용자들에게 더욱 정확하고 편리한 무역 정보 서비스를 제공할 수 있습니다. JWT 무상태 인증과 자동 알림 활성화로 사용자 경험도 크게 개선되었습니다.