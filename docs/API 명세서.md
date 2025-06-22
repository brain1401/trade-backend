# AI 기반 무역 규제 레이더 플랫폼 API 명세서 v2.2

## 1. 개요 (Overview)

본 문서는 'AI 기반 무역 규제 레이더 플랫폼'의 RESTful API를 상세히 기술한 통합 명세서입니다. 이 플랫폼은 복잡한 무역 규제, HS Code 분류, 수출입 요건, 화물 추적 등 무역 업무에 필수적인 정보들을 AI를 통해 분석하고 사용자에게 실시간으로 제공하는 것을 목표로 합니다.

**v2.2 주요 변경사항:**

- **보안 강화**: 프론트엔드 불필요 정보 완전 제거 (ID, roles, registrationType)
- **OAuth 프로필 이미지**: Google, Naver, Kakao 프로필 이미지 지원
- **에러 메시지 일반화**: 내부 정보 노출 방지를 위한 보안적 메시지 적용
- **상세 문서화**: 모든 요청/응답 필드에 대한 상세 설명 추가
- **JWT 토큰 기반 인증**: HttpOnly Cookie를 통한 JWT 토큰으로 사용자 식별

### 1.1 시스템 아키텍처 및 인증 방식

### 인증 아키텍처

```
┌─────────────────┐         ┌──────────────────┐
│   Public API    │         │  Private API     │
│ (검색/분석 기능)   │         │ (북마크/대시보드)   │
│                 │         │                  │
│ ✓ 인증 불필요     │         │ ✓ 인증 필수       │
│ ✓ 선택적 개인화   │         │ ✓ 사용자별 데이터  │
└─────────────────┘         └──────────────────┘
          │                           │
          └───────────┬─────────────────┘
                      │
              ┌──────────────────┐
              │ HttpOnly Cookie  │
              │ JWT 인증 시스템   │
              └──────────────────┘

```

### 핵심 설계 원칙

- **Public First**: 무역 정보 조회는 기본적으로 공개 서비스
- **Progressive Enhancement**: 로그인 시 추가 기능 제공
- **사용자 선택권**: 익명 사용 vs 개인화 서비스 선택 가능

### 데이터 흐름

1. **익명 사용자**: 검색 → AI 분석 → 결과 제공
2. **로그인 사용자**: 검색 → AI 분석 → 결과 제공 + 개인화 기능 (히스토리, 추천 등)
3. **개인 데이터**: 북마크 → 모니터링 → 알림 발송 (인증 필수)

### 1.2 기본 정보

- **기본 URL**: `http://localhost:8081/api`
- **프로토콜**: HTTPS (운영환경)
- **인증 방식**: JWT + HttpOnly Cookie (선택적)
- **Content-Type**: `application/json`
- **CORS**: Public API는 기본 허용, Private API는 `withCredentials: true` 필수

### 1.3 공통 응답 형식 (Common Response Wrapper)

모든 API 응답은 아래와 같은 `ApiResponse` 객체로 래핑되어 반환됩니다.

```tsx
interface ApiResponse<T> {
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

---

## 2. 인증 API (Authentication)

### 2.1 회원가입

**`POST /api/auth/register`**

신규 계정을 생성합니다. 생성된 계정은 즉시 로그인 가능한 상태가 됩니다.

### Request Body

| 필드명     | 타입   | 필수 | 설명                                  |
| ---------- | ------ | ---- | ------------------------------------- |
| `email`    | string | ✓    | 사용자 이메일 주소                    |
| `password` | string | ✓    | 사용자 비밀번호 (최소 8자 이상)       |
| `name`     | string | ✓    | 사용자 표시명 (환영 메시지 등에 사용) |

```json
{
  "email": "user@example.com",
  "password": "password123",
  "name": "홍길동"
}
```

### Response (201 Created)

| 필드명              | 타입   | 설명                                     |
| ------------------- | ------ | ---------------------------------------- |
| `success`           | string | 요청 처리 결과 ("SUCCESS" 또는 "ERROR")  |
| `message`           | string | 처리 결과 메시지                         |
| `data.email`        | string | 사용자 이메일 주소                       |
| `data.name`         | string | 사용자 표시명                            |
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

### Error Codes

- `USER_001`: 계정 생성 실패
- `USER_002`: 입력 정보 오류
- `COMMON_001`: 요청 형식 오류

---

### 2.2 로그인

**`POST /api/auth/login`**

사용자 인증을 수행하고, 성공 시 인증 쿠키를 설정합니다.

### Request Body

| 필드명       | 타입    | 필수 | 설명                             |
| ------------ | ------- | ---- | -------------------------------- |
| `email`      | string  | ✓    | 사용자 이메일 주소               |
| `password`   | string  | ✓    | 사용자 비밀번호                  |
| `rememberMe` | boolean | -    | 로그인 유지 여부 (기본값: false) |

```json
{
  "email": "user@example.com",
  "password": "password123",
  "rememberMe": true
}
```

### Response (200 OK)

| 필드명                   | 타입   | 설명                                    |
| ------------------------ | ------ | --------------------------------------- |
| `success`                | string | 요청 처리 결과 ("SUCCESS" 또는 "ERROR") |
| `message`                | string | 처리 결과 메시지                        |
| `data.user.email`        | string | 사용자 이메일 주소                      |
| `data.user.name`         | string | 사용자 표시명                           |
| `data.user.profileImage` | string | 프로필 이미지 URL (없으면 null)         |

```json
{
  "success": "SUCCESS",
  "message": "인증되었습니다",
  "data": {
    "user": {
      "email": "user@example.com",
      "name": "홍길동",
      "profileImage": null
    }
  }
}
```

### Response Headers (Set-Cookie)

인증 성공 시 HttpOnly 쿠키가 자동으로 설정됩니다:

```
Set-Cookie: token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...;
            HttpOnly;
            Secure;
            SameSite=Strict;
            Path=/;
            Max-Age=604800
```

### Cookie 설정 정책

- **rememberMe: true** → `Max-Age=604800` (7일간 유효)
- **rememberMe: false** → 세션 쿠키 (브라우저 종료시 삭제)

### Error Codes

- `AUTH_001`: 인증 실패
- `AUTH_002`: 계정 접근 제한

---

### 2.3 인증 상태 확인

**`GET /api/auth/verify`**

현재 인증 상태를 확인하고 사용자 정보를 반환합니다.

### Request Headers

브라우저에서 자동으로 전송되는 HttpOnly 쿠키를 사용:

```
Cookie: token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Response (200 OK)

| 필드명              | 타입   | 설명                                    |
| ------------------- | ------ | --------------------------------------- |
| `success`           | string | 요청 처리 결과 ("SUCCESS" 또는 "ERROR") |
| `message`           | string | 처리 결과 메시지                        |
| `data.email`        | string | 사용자 이메일 주소                      |
| `data.name`         | string | 사용자 표시명                           |
| `data.profileImage` | string | 프로필 이미지 URL (없으면 null)         |

```json
{
  "success": "SUCCESS",
  "message": "인증 상태 확인됨",
  "data": {
    "email": "user@example.com",
    "name": "홍길동",
    "profileImage": null
  }
}
```

### Error Codes

- `AUTH_003`: 인증 만료
- `AUTH_004`: 인증 오류

---

### 2.4 OAuth 소셜 로그인

**`GET /api/oauth2/authorization/{provider}`**

외부 OAuth 제공업체를 통한 소셜 로그인을 시작합니다.

### Path Parameters

| 필드명     | 타입   | 필수 | 설명                                        |
| ---------- | ------ | ---- | ------------------------------------------- |
| `provider` | string | ✓    | OAuth 제공업체 (`google`, `naver`, `kakao`) |

### Query Parameters  

| 필드명       | 타입    | 필수 | 설명                             |
| ------------ | ------- | ---- | -------------------------------- |
| `rememberMe` | boolean | -    | 로그인 유지 여부 (기본값: false) |

### Response (302 Found)

사용자를 해당 OAuth 제공업체의 인증 페이지로 리디렉션합니다.

```
Location: https://accounts.google.com/oauth/authorize?client_id=...
```

### OAuth 성공 시 콜백

인증 성공 시 다음 작업이 수행됩니다:
1. 사용자 정보 획득 (이메일, 이름, 프로필 이미지)
2. HttpOnly 쿠키 설정  
3. 프론트엔드로 리디렉션

```
Location: https://your-frontend-domain.com/auth/callback?success=true
```

**프로필 이미지 지원:**
- **Google**: `picture` 필드에서 이미지 URL 획득
- **Naver**: `profile_image` 필드에서 이미지 URL 획득  
- **Kakao**: `thumbnail_image` 필드에서 이미지 URL 획득

### OAuth 실패 시 콜백

```
Location: https://your-frontend-domain.com/auth/callback?error=oauth_failed
```

---

### 2.5 로그아웃

**`POST /api/auth/logout`**

현재 세션을 종료하고 인증을 해제합니다.

### Request Headers

브라우저에서 자동으로 전송되는 HttpOnly 쿠키를 사용:

```
Cookie: token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Response (200 OK)

| 필드명    | 타입   | 설명                                    |
| --------- | ------ | --------------------------------------- |
| `success` | string | 요청 처리 결과 ("SUCCESS" 또는 "ERROR") |
| `message` | string | 처리 결과 메시지                        |
| `data`    | null   | 로그아웃 시 반환 데이터 없음            |

```json
{
  "success": "SUCCESS",
  "message": "세션이 종료되었습니다",
  "data": null
}
```

### Response Headers (Set-Cookie)

로그아웃 시 인증 쿠키가 자동으로 삭제됩니다:

```
Set-Cookie: token=; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=0
```

---

## 3. 검색/분석 API (Search & Analysis) 🌟 PUBLIC API

> 🔓 공개 API: 이 섹션의 모든 API는 로그인 없이 사용 가능합니다.
> 
> 
> **✨ 선택적 개인화**: 로그인 상태에서 요청 시 추가 개인화 기능이 제공됩니다.
> 

### 3.1 지능형 통합 검색 (의도 분석)

**`POST /api/search/analyze`**

자연어 검색 질의를 분석하여 사용자의 의도를 파악합니다.

### Authentication (Optional)

로그인한 사용자는 개인화된 추가 기능을 이용할 수 있습니다:

```
Cookie: token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Request Body

| 필드명  | 타입   | 필수 | 설명                      |
| ------- | ------ | ---- | ------------------------- |
| `query` | string | ✓    | 사용자 검색 질의 (자연어) |

```json
{
  "query": "아이폰 15 수출 시 필요한 HS Code와 서류"
}
```

### Response (200 OK)

| 필드명                         | 타입     | 설명                                    |
| ------------------------------ | -------- | --------------------------------------- |
| `success`                      | string   | 요청 처리 결과 ("SUCCESS" 또는 "ERROR") |
| `message`                      | string   | 처리 결과 메시지                        |
| `data.intent`                  | string   | 분석된 사용자 의도 코드                 |
| `data.confidence`              | number   | 의도 분석 신뢰도 (0.0 ~ 1.0)            |
| `data.suggestedAction`         | string   | 추천 다음 단계 액션                     |
| `data.extractedKeywords`       | string[] | 질의에서 추출된 핵심 키워드             |
| `data.nextStepUrl`             | string   | 다음 단계 API 엔드포인트 URL            |
| `data.personalizedSuggestions` | string[] | 개인화된 추천 사항 (로그인 시에만 제공) |

```json
{
  "success": "SUCCESS",
  "message": "의도 분석 완료",
  "data": {
    "intent": "HS_CODE_ANALYSIS",
    "confidence": 0.95,
    "suggestedAction": "HS_CODE_ANALYSIS",
    "extractedKeywords": ["아이폰", "15", "수출", "HS Code"],
    "nextStepUrl": "/api/search/hscode/start",
    "personalizedSuggestions": [
      "이전 검색 기록을 기반으로 한 추천",
      "북마크 추가를 통한 모니터링 가능"
    ]
  }
}
```

### 🔐 로그인 시 추가 기능

- `personalizedSuggestions`: 검색 이력 기반 개인화된 추천
- `searchHistory`: 관련 이전 검색 결과  
- `bookmarkRecommendation`: 북마크 추가 추천 여부

### 의도 분류 (Intent Types)

| 의도 코드               | 설명                      |
| ----------------------- | ------------------------- |
| `HS_CODE_ANALYSIS`      | HS Code 분석 및 품목 분류 |
| `CARGO_TRACKING`        | 화물 추적 조회            |
| `GENERAL_TRADE_INQUIRY` | 일반 무역 정보 조회       |

### Error Codes

- `SEARCH_001`: 요청 데이터 오류

---

### 3.2 HS Code 분석 시작

**`POST /api/search/hscode/start`**

품목의 HS Code 분석을 위한 비동기 작업을 시작합니다.

### Authentication (Optional)

로그인한 사용자는 개인화된 추가 기능을 이용할 수 있습니다:

```
Cookie: token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Request Body

| 필드명                         | 타입   | 필수 | 설명                          |
| ------------------------------ | ------ | ---- | ----------------------------- |
| `query`                        | string | ✓    | 분석할 품목명 또는 상품 설명  |
| `additionalInfo`               | object | -    | 추가 분석 정보                |
| `additionalInfo.purpose`       | string | -    | 무역 목적 ("수출", "수입" 등) |
| `additionalInfo.targetCountry` | string | -    | 대상 국가명                   |

```json
{
  "query": "아이폰 15",
  "additionalInfo": {
    "purpose": "수출",
    "targetCountry": "미국"
  }
}
```

### Response (202 Accepted)

| 필드명               | 타입    | 설명                                         |
| -------------------- | ------- | -------------------------------------------- |
| `success`            | string  | 요청 처리 결과 ("SUCCESS" 또는 "ERROR")      |
| `message`            | string  | 처리 결과 메시지                             |
| `data.jobId`         | string  | 분석 작업 고유 식별자                        |
| `data.status`        | string  | 현재 작업 상태 ("THINKING", "PROCESSING" 등) |
| `data.estimatedTime` | number  | 예상 완료 시간 (초)                          |
| `data.streamUrl`     | string  | 실시간 결과 스트리밍 URL                     |
| `data.canBookmark`   | boolean | 북마크 추가 가능 여부 (로그인 시에만 true)   |

```json
{
  "success": "SUCCESS",
  "message": "분석을 시작합니다",
  "data": {
    "jobId": "job_123456789",
    "status": "THINKING",
    "estimatedTime": 30,
    "streamUrl": "/api/search/hscode/stream/job_123456789",
    "canBookmark": true
  }
}
```

### 🔐 로그인 시 추가 기능

- `canBookmark`: 북마크 추가 가능 (true로 설정)
- `searchHistorySaved`: 검색 이력 자동 저장
- `similarPreviousSearches`: 유사한 이전 검색 결과 제공

---

### 3.3 HS Code 분석 스트리밍

**`GET /api/search/hscode/stream/{jobId}`**

Server-Sent Events(SSE)를 사용하여 HS Code 분석 과정을 실시간으로 스트리밍합니다.

### Authentication (Optional)

```
Cookie: token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

```

### Path Parameters

- `jobId` (string, required): 분석 작업 ID

### Response Content-Type

```
Content-Type: text/event-stream
Cache-Control: no-cache
Connection: keep-alive

```

### SSE Event Stream

```
event: thinking
data: {"message": "HS Code 데이터베이스 검색 중..."}

event: stream_start
data: {"message": "분석 결과를 생성하고 있습니다"}

event: stream_data
data: {"content": "아이폰 15는 "}

event: stream_data
data: {"content": "HS Code 8517.12.00으로 "}

event: stream_data
data: {"content": "분류됩니다..."}

event: stream_end
data: {
  "finalData": {
    "hsCode": "8517.12.00",
    "description": "스마트폰 및 기타 무선전화기",
    "analysis": {
      "summary": "아이폰 15는 HS Code 8517.12.00으로 분류됩니다. 이는 '스마트폰 및 기타 무선전화기' 품목에 해당합니다.",
      "exportRequirements": [
        {
          "country": "미국",
          "requirements": ["원산지증명서", "KC 인증서"],
          "tariffRate": "0%",
          "notes": "한-미 FTA 적용 시 무관세"
        }
      ],
      "certifications": [
        {
          "name": "KC 인증",
          "description": "방송통신기자재 적합성평가",
          "required": true
        }
      ],
      "relatedNews": [
        {
          "title": "스마트폰 수출 절차 간소화 정책 발표",
          "url": "https://example.com/news/1",
          "publishedAt": "2024-01-15T09:00:00Z"
        }
      ],
      "tradeStatistics": {
        "yearlyExport": {
          "2023": "125억 달러",
          "2022": "118억 달러"
        },
        "topDestinations": ["미국", "중국", "베트남"]
      }
    },
    "sources": [
      {
        "title": "관세청 품목분류",
        "url": "https://unipass.customs.go.kr",
        "snippet": "8517.12.00 - 스마트폰 및 기타 무선전화기",
        "type": "OFFICIAL"
      }
    ],
    "bookmarkButton": {
      "enabled": true,
      "text": "이 HS Code를 북마크에 추가",
      "requiresLogin": false
    },
    "personalizedInsights": [
      "회원님이 관심 있어하시는 전자제품 카테고리입니다",
      "이전 검색한 8517.11.00 품목과 유사합니다"
    ]
  }
}

```

### 🔐 로그인 시 추가 기능

- `personalizedInsights`: 개인화된 인사이트
- `relatedBookmarks`: 관련 기존 북마크
- `searchHistorySaved`: 검색 이력 저장 여부

### Error Codes

- `SEARCH_002`: 분석 작업을 찾을 수 없음
- `SEARCH_003`: 분석 작업이 실패함

---

### 3.4 화물 추적 조회

**`GET /api/search/cargo/{cargoNumber}`**

화물관리번호를 이용해 통관 진행 상태를 조회합니다.

### Authentication (Optional)

```
Cookie: token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

```

### Path Parameters

- `cargoNumber` (string, required): 17자리 화물관리번호

### Response (200 OK)

```json
{
  "success": "SUCCESS",
  "message": "화물 정보 조회 완료",
  "data": {
    "cargoNumber": "12345678901234567",
    "status": "통관 진행중",
    "currentStep": "검사 대기",
    "progressPercentage": 60,
    "steps": [
      {
        "step": "신고 접수",
        "status": "완료",
        "timestamp": "2024-01-15T09:00:00Z",
        "description": "수입신고서가 정상적으로 접수되었습니다"
      },
      {
        "step": "서류 심사",
        "status": "완료",
        "timestamp": "2024-01-15T09:30:00Z",
        "description": "제출 서류 심사가 완료되었습니다"
      },
      {
        "step": "검사 대기",
        "status": "진행중",
        "timestamp": "2024-01-15T10:30:00Z",
        "description": "물품 검사를 대기하고 있습니다"
      },
      {
        "step": "통관 완료",
        "status": "대기",
        "timestamp": null,
        "description": "검사 완료 후 통관 처리됩니다"
      }
    ],
    "estimatedCompletion": "2024-01-16T14:00:00Z",
    "additionalInfo": {
      "declarationType": "수입신고",
      "customs": "인천본부세관",
      "declarer": "(주)무역회사"
    },
    "bookmarkButton": {
      "enabled": true,
      "text": "이 화물을 추적 목록에 추가",
      "requiresLogin": false
    }
  }
}

```

### 🔐 로그인 시 추가 기능

- `trackingAlerts`: 상태 변경 알림 설정 가능
- `relatedCargos`: 관련 화물 목록
- `estimatedCosts`: 예상 비용 정보

### Error Codes

- `SEARCH_004`: 화물번호가 유효하지 않음
- `SEARCH_005`: 화물 정보를 찾을 수 없음

---

### 3.5 일반 무역 정보 조회 시작

**`POST /api/search/general/start`**

일반 무역 정보(뉴스, 정책 등) 조회를 위한 비동기 작업을 시작합니다.

### Authentication (Optional)

```
Cookie: token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

```

### Request Body

```json
{
  "query": "최근 미국의 관세 정책 변화에 대해 알려주세요",
  "additionalContext": {
    "region": "미국",
    "timeframe": "최근 3개월",
    "topics": ["관세정책", "무역협정"]
  }
}

```

### Response (202 Accepted)

```json
{
  "success": "SUCCESS",
  "message": "일반 무역 정보 조회를 시작합니다",
  "data": {
    "jobId": "job_987654321",
    "status": "THINKING",
    "estimatedTime": 25,
    "streamUrl": "/api/search/general/stream/job_987654321"
  }
}

```

---

### 3.6 일반 무역 정보 조회 스트리밍

**`GET /api/search/general/stream/{jobId}`**

SSE를 사용하여 일반 무역 정보 조회 결과를 실시간으로 스트리밍합니다.

### Authentication (Optional)

```
Cookie: token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

```

### Response Content-Type

```
Content-Type: text/event-stream

```

### SSE `stream_end` 예시

```json
{
  "finalData": {
    "answer": "최근 미국의 관세 정책은 다음과 같은 주요 변화가 있었습니다:\n\n1. **중국산 제품 관세 조정**: 2024년 1월부터 일부 중국산 소비재에 대한 관세율이 15%에서 10%로 인하되었습니다.\n\n2. **친환경 제품 우대**: 태양광 패널, 전기차 배터리 등 친환경 제품에 대한 관세 면제 조치가 확대되었습니다.",
    "relatedNews": [
      {
        "title": "미국 관세청, 2024년 새로운 관세 정책 발표",
        "url": "https://example.com/news/us-tariff-2024",
        "sourceName": "미국 관세청",
        "publishedAt": "2024-01-10T09:00:00Z",
        "summary": "중국산 일부 제품 관세 인하 및 친환경 제품 우대 정책 발표"
      }
    ],
    "sources": [
      {
        "title": "미국 관세청 공식 발표",
        "url": "https://cbp.gov/trade/tariff/2024-updates",
        "type": "OFFICIAL",
        "reliability": "HIGH"
      },
      {
        "title": "무역협회 분석 보고서",
        "url": "https://trade-association.org/us-tariff-analysis",
        "type": "ANALYSIS",
        "reliability": "MEDIUM"
      }
    ],
    "relatedTopics": [
      "미국-중국 무역관계",
      "WTO 규정 변화",
      "친환경 무역정책"
    ],
    "recommendations": [
      "관세율 변경이 예상되는 품목의 경우 사전 수입 계획 수립 권장",
      "친환경 인증이 있는 제품의 경우 관세 혜택 신청 검토"
    ],
    "personalizedRecommendations": [
      "회원님이 북마크하신 전자제품 품목도 이번 정책 변경 대상입니다",
      "정기적인 모니터링을 위해 관세정책 키워드를 북마크에 추가해보세요"
    ]
  }
}

```

### 🔐 로그인 시 추가 기능

- `personalizedRecommendations`: 개인화된 추천사항
- `relatedUserContent`: 사용자의 관련 북마크/검색 이력
- `alertSuggestions`: 알림 설정 제안

---

## 4. 북마크 API (Bookmark Management) 🔒 PRIVATE API

> 🔐 인증 필수: 이 섹션의 모든 API는 로그인이 필요합니다.
> 

### 4.1 북마크 목록 조회

**`GET /api/bookmarks`**

현재 사용자가 저장한 북마크 목록을 조회합니다.

### Authentication (Required)

인증된 사용자만 접근 가능합니다:

```
Cookie: token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Query Parameters

| 필드명   | 타입    | 필수 | 설명                                         |
| -------- | ------- | ---- | -------------------------------------------- |
| `type`   | string  | -    | 북마크 타입 필터 (`HS_CODE`, `CARGO`, `ALL`) |
| `offset` | integer | -    | 페이지 오프셋 (기본값: 0)                    |
| `limit`  | integer | -    | 페이지 크기 (기본값: 20, 최대: 100)          |
| `sort`   | string  | -    | 정렬 기준 (`createdAt`, `updatedAt`, `name`) |
| `order`  | string  | -    | 정렬 순서 (`asc`, `desc`, 기본값: desc)      |

### Response (200 OK)

| 필드명                             | 타입    | 설명                                    |
| ---------------------------------- | ------- | --------------------------------------- |
| `success`                          | string  | 요청 처리 결과 ("SUCCESS" 또는 "ERROR") |
| `message`                          | string  | 처리 결과 메시지                        |
| `data.content[]`                   | array   | 북마크 아이템 목록                      |
| `data.content[].bookmarkId`        | string  | 북마크 고유 식별자                      |
| `data.content[].type`              | string  | 북마크 타입 ("HS_CODE" 또는 "CARGO")    |
| `data.content[].targetValue`       | string  | 북마크 대상 값 (HS Code 또는 화물번호)  |
| `data.content[].displayName`       | string  | 사용자 지정 표시명                      |
| `data.content[].description`       | string  | 북마크 설명                             |
| `data.content[].monitoringEnabled` | boolean | 모니터링 활성화 여부                    |
| `data.content[].alertCount`        | number  | 받은 알림 횟수                          |
| `data.content[].lastAlert`         | string  | 마지막 알림 시간 (ISO 8601)             |
| `data.content[].createdAt`         | string  | 생성 시간 (ISO 8601)                    |
| `data.content[].updatedAt`         | string  | 수정 시간 (ISO 8601)                    |
| `data.pagination`                  | object  | 페이지네이션 정보                       |
| `data.pagination.offset`           | number  | 현재 오프셋                             |
| `data.pagination.limit`            | number  | 페이지 크기                             |
| `data.pagination.total`            | number  | 전체 아이템 수                          |
| `data.pagination.hasNext`          | boolean | 다음 페이지 존재 여부                   |
| `data.pagination.hasPrevious`      | boolean | 이전 페이지 존재 여부                   |

```json
{
  "success": "SUCCESS",
  "message": "북마크 목록 조회됨",
  "data": {
    "content": [
      {
        "bookmarkId": "bm_001",
        "type": "HS_CODE",
        "targetValue": "8517.12.00",
        "displayName": "스마트폰 (아이폰 15)",
        "description": "스마트폰 및 기타 무선전화기",
        "monitoringEnabled": true,
        "alertCount": 3,
        "lastAlert": "2024-01-15T10:00:00Z",
        "createdAt": "2024-01-10T09:00:00Z",
        "updatedAt": "2024-01-15T09:30:00Z"
      },
      {
        "bookmarkId": "bm_002",
        "type": "CARGO",
        "targetValue": "12345678901234567",
        "displayName": "1월 수입 화물",
        "description": "전자제품 수입 화물",
        "monitoringEnabled": false,
        "alertCount": 0,
        "lastAlert": null,
        "createdAt": "2024-01-08T14:20:00Z",
        "updatedAt": "2024-01-08T14:20:00Z"
      }
    ],
    "pagination": {
      "offset": 0,
      "limit": 20,
      "total": 2,
      "hasNext": false,
      "hasPrevious": false
    }
  }
}
```

### Error Codes

- `AUTH_003`: 인증 만료
- `AUTH_004`: 인증 오류

---

### 4.2 북마크 추가

**`POST /api/bookmarks`**

새로운 아이템을 북마크에 추가합니다.

### Authentication (Required)

인증된 사용자만 접근 가능합니다:

```
Cookie: token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Request Body

| 필드명              | 타입    | 필수 | 설명                                     |
| ------------------- | ------- | ---- | ---------------------------------------- |
| `type`              | string  | ✓    | 북마크 타입 ("HS_CODE" 또는 "CARGO")     |
| `targetValue`       | string  | ✓    | 북마크할 대상 값 (HS Code 또는 화물번호) |
| `displayName`       | string  | ✓    | 사용자 지정 표시명                       |
| `description`       | string  | -    | 북마크 설명                              |
| `monitoringEnabled` | boolean | -    | 모니터링 활성화 여부 (기본값: false)     |

```json
{
  "type": "HS_CODE",
  "targetValue": "8517.12.00",
  "displayName": "스마트폰 (아이폰 15)",
  "description": "스마트폰 수출용 품목",
  "monitoringEnabled": true
}
```

### Response (201 Created)

| 필드명                   | 타입    | 설명                                    |
| ------------------------ | ------- | --------------------------------------- |
| `success`                | string  | 요청 처리 결과 ("SUCCESS" 또는 "ERROR") |
| `message`                | string  | 처리 결과 메시지                        |
| `data.bookmarkId`        | string  | 생성된 북마크 고유 식별자               |
| `data.type`              | string  | 북마크 타입                             |
| `data.targetValue`       | string  | 북마크 대상 값                          |
| `data.displayName`       | string  | 사용자 지정 표시명                      |
| `data.description`       | string  | 북마크 설명                             |
| `data.monitoringEnabled` | boolean | 모니터링 활성화 여부                    |
| `data.alertCount`        | number  | 알림 횟수 (초기값: 0)                   |
| `data.lastAlert`         | string  | 마지막 알림 시간 (초기값: null)         |
| `data.createdAt`         | string  | 생성 시간 (ISO 8601)                    |
| `data.updatedAt`         | string  | 수정 시간 (ISO 8601)                    |

```json
{
  "success": "SUCCESS",
  "message": "북마크가 추가됨",
  "data": {
    "bookmarkId": "bm_003",
    "type": "HS_CODE",
    "targetValue": "8517.12.00",
    "displayName": "스마트폰 (아이폰 15)",
    "description": "스마트폰 수출용 품목",
    "monitoringEnabled": true,
    "alertCount": 0,
    "lastAlert": null,
    "createdAt": "2024-01-16T10:30:00Z",
    "updatedAt": "2024-01-16T10:30:00Z"
  }
}
```

### Error Codes

- `AUTH_003`: 토큰 만료
- `AUTH_004`: 유효하지 않은 토큰
- `BOOKMARK_002`: 이미 존재하는 북마크
- `BOOKMARK_003`: 유효하지 않은 대상 값

---

### 4.3 북마크 수정

**`PUT /api/bookmarks/{bookmarkId}`**

북마크의 표시 이름이나 모니터링 설정을 수정합니다.

### Authentication (Required)

```
Cookie: token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

```

### Path Parameters

- `bookmarkId` (integer, required): 수정할 북마크의 ID

### Request Body

```json
{
  "displayName": "Apple iPhone 15 Pro Series",
  "description": "최신 아이폰 시리즈 수출 품목",
  "monitoringEnabled": false
}

```

### Response (200 OK)

```json
{
  "success": "SUCCESS",
  "message": "북마크가 수정되었습니다",
  "data": {
    "id": 1,
    "type": "HS_CODE",
    "targetValue": "8517.12.00",
    "displayName": "Apple iPhone 15 Pro Series",
    "description": "최신 아이폰 시리즈 수출 품목",
    "monitoringEnabled": false,
    "alertCount": 3,
    "lastAlert": "2024-01-15T10:00:00Z",
    "createdAt": "2024-01-10T09:00:00Z",
    "updatedAt": "2024-01-16T11:00:00Z"
  }
}

```

### Error Codes

- `AUTH_003`: 토큰 만료
- `AUTH_004`: 유효하지 않은 토큰
- `BOOKMARK_001`: 북마크를 찾을 수 없음
- `AUTH_005`: 권한이 없음 (다른 사용자의 북마크)

---

### 4.4 북마크 삭제

**`DELETE /api/bookmarks/{bookmarkId}`**

지정된 북마크를 삭제합니다.

### Authentication (Required)

```
Cookie: token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

```

### Path Parameters

- `bookmarkId` (integer, required): 삭제할 북마크의 ID

### Response (200 OK)

```json
{
  "success": "SUCCESS",
  "message": "북마크가 삭제되었습니다",
  "data": null
}

```

### Error Codes

- `AUTH_003`: 토큰 만료
- `AUTH_004`: 유효하지 않은 토큰
- `BOOKMARK_001`: 북마크를 찾을 수 없음
- `AUTH_005`: 권한이 없음

---

## 5. 대시보드 API (Dashboard) 🔒 PRIVATE API

> 🔐 인증 필수: 이 섹션의 모든 API는 로그인이 필요합니다.
> 

### 5.1 업데이트 피드 조회

**`GET /api/dashboard/feeds`**

북마크한 항목에 대한 변동사항을 시간순으로 조회합니다.

### Authentication (Required)

```
Cookie: token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

```

### Query Parameters

- `offset` (integer, optional): 페이지 오프셋 (기본값: 0)
- `limit` (integer, optional): 페이지 크기 (기본값: 20)
- `unreadOnly` (boolean, optional): true일 경우 읽지 않은 피드만 조회
- `feedType` (string, optional): 피드 타입 필터

### Response (200 OK)

```json
{
  "success": "SUCCESS",
  "message": "업데이트 피드 조회 완료",
  "data": {
    "content": [
      {
        "id": 1,
        "feedType": "HS_CODE_TARIFF_CHANGE",
        "targetType": "HS_CODE",
        "targetValue": "8517.12.00",
        "title": "스마트폰 관세율 변경",
        "content": "미국향 스마트폰 관세율이 5%에서 3%로 인하되었습니다.",
        "changeDetails": {
          "previous": "5%",
          "current": "3%",
          "effectiveDate": "2024-01-15T00:00:00Z"
        },
        "sourceUrl": "https://example.com/tariff-update",
        "importance": "HIGH",
        "isRead": false,
        "createdAt": "2024-01-15T10:00:00Z"
      },
      {
        "id": 2,
        "feedType": "CARGO_STATUS_UPDATE",
        "targetType": "CARGO",
        "targetValue": "12345678901234567",
        "title": "화물 통관 완료",
        "content": "등록하신 화물의 통관이 완료되었습니다.",
        "changeDetails": {
          "previous": "검사 대기",
          "current": "통관 완료",
          "completedAt": "2024-01-15T14:30:00Z"
        },
        "sourceUrl": null,
        "importance": "MEDIUM",
        "isRead": true,
        "createdAt": "2024-01-15T14:35:00Z"
      }
    ],
    "pagination": {
      "offset": 0,
      "limit": 20,
      "total": 2,
      "hasNext": false
    },
    "summary": {
      "totalUnread": 1,
      "totalHigh": 1,
      "totalMedium": 1,
      "totalLow": 0
    }
  }
}

```

### 피드 타입 (Feed Types)

- `HS_CODE_TARIFF_CHANGE`: 관세율 변경
- `HS_CODE_REGULATION_UPDATE`: 규제 정보 업데이트
- `CARGO_STATUS_UPDATE`: 화물 상태 변경
- `TRADE_NEWS`: 관련 무역 뉴스
- `POLICY_UPDATE`: 정책 변경 사항

### Error Codes

- `AUTH_003`: 토큰 만료
- `AUTH_004`: 유효하지 않은 토큰

---

### 5.2 피드 읽음 처리

**`PUT /api/dashboard/feeds/{feedId}/read`**

특정 업데이트 피드를 읽음 상태로 변경합니다.

### Authentication (Required)

```
Cookie: token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

```

### Path Parameters

- `feedId` (integer, required): 피드 ID

### Response (200 OK)

```json
{
  "success": "SUCCESS",
  "message": "피드가 읽음 처리되었습니다",
  "data": null
}

```

### Error Codes

- `AUTH_003`: 토큰 만료
- `AUTH_004`: 유효하지 않은 토큰

---

### 5.3 피드 일괄 읽음 처리

**`PUT /api/dashboard/feeds/read-all`**

모든 읽지 않은 피드를 읽음 상태로 변경합니다.

### Authentication (Required)

```
Cookie: token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

```

### Response (200 OK)

```json
{
  "success": "SUCCESS",
  "message": "모든 피드가 읽음 처리되었습니다",
  "data": {
    "processedCount": 5
  }
}

```

### Error Codes

- `AUTH_003`: 토큰 만료
- `AUTH_004`: 유효하지 않은 토큰

---

### 5.4 대시보드 요약 정보

**`GET /api/dashboard/summary`**

대시보드 상단에 표시될 요약 정보를 조회합니다.

### Authentication (Required)

```
Cookie: token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

```

### Response (200 OK)

```json
{
  "success": "SUCCESS",
  "message": "대시보드 요약 정보",
  "data": {
    "bookmarks": {
      "total": 5,
      "activeMonitoring": 3,
      "byType": {
        "HS_CODE": 3,
        "CARGO": 2
      }
    },
    "feeds": {
      "unreadCount": 2,
      "todayCount": 5,
      "weekCount": 12,
      "byImportance": {
        "HIGH": 1,
        "MEDIUM": 3,
        "LOW": 8
      }
    },
    "recentActivity": [
      {
        "type": "BOOKMARK_ADDED",
        "message": "새로운 HS Code 북마크 추가: 8517.12.00",
        "timestamp": "2024-01-15T09:30:00Z"
      },
      {
        "type": "FEED_RECEIVED",
        "message": "관세율 변경 알림 수신",
        "timestamp": "2024-01-15T10:00:00Z"
      }
    ],
    "quickStats": {
      "searchCount": 25,
      "totalSavedTime": "2.5시간",
      "accuracyRate": "96%"
    }
  }
}

```

### Error Codes

- `AUTH_003`: 토큰 만료
- `AUTH_004`: 유효하지 않은 토큰

---

## 6. 공통 에러 코드 (Error Codes)

> **보안 정책**: 모든 에러 메시지는 시스템 내부 정보 노출을 방지하기 위해 일반적인 형태로 제공됩니다.

### 6.1 인증 관련 (AUTH_xxx)

| 코드       | 메시지         | 설명                                            | HTTP 상태 |
| ---------- | -------------- | ----------------------------------------------- | --------- |
| `AUTH_001` | 인증 실패      | 제공된 인증 정보가 올바르지 않습니다            | 401       |
| `AUTH_002` | 계정 접근 제한 | 현재 계정에 일시적인 접근 제한이 적용되었습니다 | 423       |
| `AUTH_003` | 인증 만료      | 인증 정보가 만료되었습니다                      | 401       |
| `AUTH_004` | 인증 오류      | 인증 처리 중 오류가 발생했습니다                | 401       |
| `AUTH_005` | 접근 권한 없음 | 해당 리소스에 접근할 권한이 없습니다            | 403       |

### 6.2 사용자 관련 (USER_xxx)

| 코드       | 메시지           | 설명                                  | HTTP 상태 |
| ---------- | ---------------- | ------------------------------------- | --------- |
| `USER_001` | 계정 생성 실패   | 계정 생성 중 오류가 발생했습니다      | 409       |
| `USER_002` | 입력 정보 오류   | 제공된 정보가 올바르지 않습니다       | 400       |
| `USER_003` | 사용자 정보 없음 | 요청한 사용자 정보를 찾을 수 없습니다 | 404       |

### 6.3 검색 관련 (SEARCH_xxx)

| 코드         | 메시지              | 설명                                 | HTTP 상태 |
| ------------ | ------------------- | ------------------------------------ | --------- |
| `SEARCH_001` | 요청 데이터 오류    | 검색 요청 데이터가 올바르지 않습니다 | 400       |
| `SEARCH_002` | 작업을 찾을 수 없음 | 요청한 작업을 찾을 수 없습니다       | 404       |
| `SEARCH_003` | 처리 실패           | 요청 처리 중 오류가 발생했습니다     | 500       |
| `SEARCH_004` | 입력 형식 오류      | 제공된 입력 형식이 올바르지 않습니다 | 400       |
| `SEARCH_005` | 정보를 찾을 수 없음 | 요청한 정보를 찾을 수 없습니다       | 404       |

### 6.4 북마크 관련 (BOOKMARK_xxx)

| 코드           | 메시지             | 설명                              | HTTP 상태 |
| -------------- | ------------------ | --------------------------------- | --------- |
| `BOOKMARK_001` | 북마크 없음        | 요청한 북마크를 찾을 수 없습니다  | 404       |
| `BOOKMARK_002` | 중복 북마크        | 이미 존재하는 북마크입니다        | 409       |
| `BOOKMARK_003` | 북마크 데이터 오류 | 북마크 데이터가 올바르지 않습니다 | 400       |

### 6.5 공통 에러 (COMMON_xxx)

| 코드         | 메시지         | 설명                                        | HTTP 상태 |
| ------------ | -------------- | ------------------------------------------- | --------- |
| `COMMON_001` | 요청 형식 오류 | 요청 형식이 올바르지 않습니다               | 400       |
| `COMMON_002` | 서버 오류      | 서버에서 오류가 발생했습니다                | 500       |
| `COMMON_003` | 요청 크기 초과 | 요청 데이터 크기가 허용 한도를 초과했습니다 | 413       |
| `COMMON_004` | 요청 한도 초과 | API 호출 한도를 초과했습니다                | 429       |

### 에러 응답 예시

모든 에러는 다음과 같은 일관된 형식으로 반환됩니다:

```json
{
  "success": "ERROR",
  "message": "인증 실패",
  "data": null
}
```

---

## 7. 보안 정책 (Security Policies)

### 7.1 쿠키 보안 설정

모든 인증 관련 쿠키는 다음 보안 정책을 준수합니다:

```
Set-Cookie: token=jwt_token;
            HttpOnly;
            Secure;
            SameSite=Strict;
            Path=/;
            Max-Age=604800

```

### 쿠키 속성 설명

- **HttpOnly**: JavaScript 접근 방지 (XSS 차단)
- **Secure**: HTTPS 연결에서만 전송
- **SameSite=Strict**: CSRF 공격 방지
- **Path=/**: 전체 도메인에서 사용 가능
- **Max-Age**: Remember Me 설정에 따라 결정

### 7.2 CORS 정책

### Public API (검색/분석)

```
Access-Control-Allow-Origin: *
Access-Control-Allow-Credentials: false
Access-Control-Allow-Methods: GET, POST, OPTIONS
Access-Control-Allow-Headers: Content-Type

```

### Private API (북마크/대시보드)

```
Access-Control-Allow-Origin: https://your-frontend-domain.com
Access-Control-Allow-Credentials: true
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Authorization

```

### 7.3 Rate Limiting

### Public API 제한

- **익명 사용자**: IP당 시간당 200회
- **검색 API**: IP당 분당 30회
- **스트리밍 API**: IP당 동시 연결 3개까지

### Private API 제한

- **일반 API**: 사용자당 분당 100회
- **대시보드 API**: 사용자당 분당 50회

---

## 8. 환경별 설정 (Environment Configuration)

### 8.1 개발 환경

```
Base URL: http://localhost:8081/api
Cookie Secure: false
HTTPS: false
Debug Mode: true
Public API Rate Limit: 1000/hour
Private API Rate Limit: 500/hour

```

### 8.2 운영 환경

```
Base URL: https://api.trade-radar.com/api
Cookie Secure: true
HTTPS: true
Debug Mode: false
Public API Rate Limit: 200/hour
Private API Rate Limit: 100/hour
CDN: CloudFlare
WAF: 활성화

```

---

## 9. 버전 관리 (API Versioning)

### 9.1 버전 정책

- **현재 버전**: v2.2
- **하위 호환성**: v2.1 API는 2024년 12월까지 지원, v2.0 API는 2024년 9월까지 지원
- **버전 명시**: URL 경로에 버전 포함 시 해당 버전 API 사용

### 9.2 마이그레이션 가이드 (v2.1 → v2.2) 🆕

#### **Breaking Changes:**

1. **User 타입 필드 제거**:
   ```typescript
   // v2.1 이전
   type User = {
     id: number;           // ❌ 제거됨
     email: string;
     name: string;
     roles: string[];      // ❌ 제거됨  
     registrationType: string; // ❌ 제거됨
   };
   
   // v2.2
   type User = {
     email: string;        // ✅ 유지
     name: string;         // ✅ 유지
     profileImage?: string; // 🆕 OAuth 프로필 이미지
   };
   ```

2. **에러 메시지 일반화**:
   ```json
   // v2.1 이전
   { "message": "이메일 또는 비밀번호가 일치하지 않습니다" }
   
   // v2.2
   { "message": "인증 실패" }
   ```

3. **북마크 ID 타입 변경**:
   ```typescript
   // v2.1 이전: number 타입
   { "id": 123 }
   
   // v2.2: string 타입
   { "bookmarkId": "bm_123" }
   ```

#### **새로운 기능:**

- **OAuth 프로필 이미지**: Google, Naver, Kakao 프로필 이미지 지원
- **상세 필드 문서화**: 모든 요청/응답 필드에 대한 상세 설명 테이블
- **보안 강화**: 내부 시스템 정보 노출 방지

### 9.3 마이그레이션 가이드 (v2.0 → v2.1)

- **검색 API**: 인증 필수 → 선택적 인증으로 변경
- **개인화 기능**: 로그인 시에만 제공되는 추가 기능 신설
- **Rate Limiting**: Public/Private API 별도 제한 정책 적용
- **CORS 정책**: Public API는 모든 도메인 허용으로 변경

---

## 10. 사용 예시 (Usage Examples)

### 10.1 익명 사용자 플로우

```jsx
// 1. 로그인 없이 HS Code 검색
const searchResponse = await fetch('/api/search/analyze', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ query: '아이폰 수출' })
});

// 2. 스트리밍으로 결과 받기
const eventSource = new EventSource('/api/search/hscode/stream/job_123');
eventSource.onmessage = (event) => {
  const data = JSON.parse(event.data);
  console.log(data.content);
};

// 3. 북마크하려면 로그인 필요 안내
if (data.bookmarkButton.requiresLogin) {
  showLoginPrompt('북마크 기능을 사용하려면 로그인해주세요');
}

```

### 10.2 로그인 사용자 플로우

```jsx
// 1. 로그인 후 쿠키 자동 설정
const loginResponse = await fetch('/api/auth/login', {
  method: 'POST',
  credentials: 'include',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ 
    email: 'user@example.com', 
    password: 'password123' 
  })
});

// 로그인 성공 시 사용자 정보 확인
if (loginResponse.success === 'SUCCESS') {
  const { email, name, profileImage } = loginResponse.data.user;
  console.log(`${name}님 환영합니다! (${email})`);
  
  // OAuth 프로필 이미지가 있다면 표시
  if (profileImage) {
    displayUserAvatar(profileImage);
  }
}

// 2. 개인화된 검색 (쿠키 자동 포함)
const searchResponse = await fetch('/api/search/analyze', {
  method: 'POST',
  credentials: 'include',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ query: '아이폰 수출' })
});

// 3. 개인화 기능 활용
if (searchResponse.data.personalizedSuggestions) {
  console.log('개인화된 추천:', searchResponse.data.personalizedSuggestions);
}

// 4. 북마크 추가 (bookmarkId로 응답)
const bookmarkResponse = await fetch('/api/bookmarks', {
  method: 'POST',
  credentials: 'include',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    type: 'HS_CODE',
    targetValue: '8517.12.00',
    displayName: '아이폰 15',
    monitoringEnabled: true
  })
});

// 북마크 ID로 향후 관리
if (bookmarkResponse.success === 'SUCCESS') {
  const bookmarkId = bookmarkResponse.data.bookmarkId;
  console.log(`북마크 생성됨: ${bookmarkId}`);
}
```

---

**🎉 보안 강화 및 사용자 경험 개선을 완료한 API 명세서 v2.2 완성!**

**✨ v2.2 주요 개선사항:**

- 🛡️ **보안 대폭 강화**: 프론트엔드 불필요 정보 완전 제거 (ID, roles, registrationType)
- 🔒 **에러 메시지 보안화**: 내부 시스템 정보 노출 방지를 위한 일반적 메시지 적용
- 📱 **OAuth 프로필 이미지**: Google, Naver, Kakao 프로필 이미지 완벽 지원
- 📊 **상세 API 문서화**: 모든 요청/응답 필드에 대한 상세 설명 테이블 추가
- 🔐 **JWT 토큰 기반 인증**: HttpOnly Cookie를 통한 JWT 토큰으로 사용자 식별하여 보안성 향상

**🛡️ v2.2 보안 개선사항:**

- ❌ **제거된 필드**: `id`, `roles`, `registrationType` (프론트엔드 불필요, 보안상 위험)
- ✅ **유지된 필드**: `email`, `name`, `profileImage`(OAuth용, 선택적)
- 🔒 **에러 메시지 보안화**: "이메일 또는 비밀번호 불일치" → "인증 실패"로 일반화
- 📊 **북마크 ID 문자열화**: `id: number` → `bookmarkId: string`으로 예측 불가능하게 변경
- 📝 **상세 문서화**: 모든 API 필드에 대한 용도별 상세 설명 테이블 추가
- 🔐 **최소 권한 원칙**: 클라이언트에 필요한 최소한의 정보만 제공하는 설계 구현