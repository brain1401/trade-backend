### AI 기능 구현 계획서 v1.0

**과제명**: AI 기반 무역 규제 레이더 플랫폼: Python AI 서버 연동 기능 구현
**문서 버전**: 1.0
**작성일**: 2025-07-05
**작성자**: Senior Software Architect & QA Lead

---

#### **1. 개요**
본 문서는 외부 Python AI 서버와의 REST API 연동을 통해 AI 기반 채팅, 뉴스 수집, 실시간 알림 기능을 Java 백엔드에 통합하기 위한 단계별 구현 계획을 기술한다. 모든 구현은 `AI 기반 무역 규제 레이더 플랫폼 API 명세서 v6.1`의 요구사항과 `dto-convention.mdc`의 코딩 규칙을 철저히 준수하는 것을 목표로 한다.

---

#### **2. 단계별 구현 계획**

### **Step 0: 사전 준비 및 환경 설정**

**목표**: Python AI 서버와 안정적으로 통신하기 위한 기본 환경을 구축한다.

**주요 작업**:
1.  **`WebClient` 설정**:
    *   **파일 생성**: `src/main/java/com/hscoderadar/config/WebClientConfig.java`
    *   **구현 내용**: Python AI 서버의 `base-url`, `connect-timeout`, `read-timeout` 등을 포함하는 `WebClient` Spring Bean을 등록한다.
2.  **설정 정보 추가**:
    *   **파일 수정**: `src/main/resources/application.properties` (및 `application-dev.properties` 등 환경별 파일)
    *   **구현 내용**: Python 서버의 주소 및 필요 시 API 키를 추가한다. (예: `ai.python.server.url=http://localhost:8000`)
3.  **패키지 구조 생성**:
    *   **경로**: `src/main/java/com/hscoderadar/domain/chat/`
    *   **작업 내용**: `controller`, `service`, `dto/request`, `dto/response`, `entity`, `repository` 디렉토리가 `dto-convention.mdc`에 명시된 대로 존재하는지 확인하고, 없다면 생성한다.

---

### **Step 1: AI 채팅 FACADE 엔드포인트 구현**

**목표**: Python AI 서버의 채팅 스트림을 `API 명세서 v6.1`의 요구사항에 맞는 복합 SSE(Server-Sent Events) 스트림으로 변환하여 프론트엔드에 제공하는 핵심 Facade 엔드포인트를 구현한다.

**주요 작업**:
1.  **Controller (`ChatController.java` 생성/수정)**:
    *   `POST /api/chat` 엔드포인트를 생성하고 `SseEmitter`를 반환 타입으로 지정한다.
    *   인증된 사용자인지 판별하기 위해 `Authorization` 헤더를 선택적으로 받도록 설정한다.
2.  **DTO 생성 (`domain/chat/dto/**`)**:
    *   `ChatRequest.java`: 프론트엔드로부터 받는 채팅 요청 DTO.
    *   `PythonChatRequest.java`: Python AI 서버로 보낼 채팅 요청 DTO.
    *   `API 명세서 v6.1`에 명시된 SSE 이벤트(`initial_metadata`, `session_info`, `thinking_*` 등)에 필요한 다수의 `response` DTO들을 `record` 형식으로 정의한다.
3.  **Service (`ChatService.java` 생성/수정)**:
    *   `WebClient`를 사용하여 Python 서버의 `POST /api/v1/chat/`를 호출하고, `Flux<String>` 형태로 응답 스트림을 구독한다.
    *   **회원/비회원 차별화 로직**: JWT 토큰 존재 여부로 사용자를 판별한다. 회원일 경우, `chat_sessions` 테이블에 새로운 세션을 생성하고 DB에 저장한다.
    *   **SSE 이벤트 오케스트레이션**: Python 서버로부터 받은 단순 `token`, `finish` 이벤트를 `API 명세서 v6.1`에 정의된 복합적인 SSE 이벤트 (`initial_metadata`, `thinking_*`, `main_message_*`, `detail_page_button_ready` 등)로 변환하여 `SseEmitter`를 통해 프론트엔드로 전송한다.
    *   **3단계 병렬 처리**: `CompletableFuture` 또는 `Project Reactor`를 사용하여 [1] AI 응답 스트리밍, [2] 상세 정보 준비, [3] 회원 대화 기록 저장을 동시에 처리하는 로직을 구현한다.

---

### **Step 2: 뉴스 데이터 수집 스케줄러 구현**

**목표**: 정해진 시간에 맞춰 Python AI 서버에 뉴스 생성을 요청하고, 그 결과를 DB에 저장하는 자동화된 스케줄러를 구현한다.

**주요 작업**:
1.  **Service (`TradeNewsService.java` 수정 또는 `NewsGenerationService.java` 생성)**:
    *   `@Scheduled(cron = "...")` 애너테이션을 사용하여 주기적으로 실행될 메소드를 생성한다. (예: 매일 새벽 1시에 실행)
    *   메소드 내부에서 `WebClient`를 사용하여 Python 서버의 `POST /api/v1/news/` 엔드포인트를 호출한다.
    *   반환된 `NewsResponse` DTO를 `TradeNews` 엔티티로 매핑하여 `TradeNewsRepository`를 통해 DB에 저장한다.
    *   외부 API 호출 실패, DB 저장 실패 등 예외 발생 시, 에러 로그를 상세히 기록하고 트랜잭션을 롤백하여 데이터 정합성을 유지한다.

---

### **Step 3: 실시간 모니터링 알림 소비자(CONSUMER) 구현**

**목표**: Python AI 서버(Producer)가 Redis 큐에 등록한 알림 작업을 실시간으로 처리하는 안정적인 소비자(Consumer)를 구현하여, 신뢰성 있는 알림 발송 시스템을 완성한다.

**주요 작업**:
1.  **Service/Listener (`NotificationConsumerService.java` 또는 `RedisQueueListener.java` 생성)**:
    *   `ApplicationRunner`를 구현하여 애플리케이션 시작 시 백그라운드 스레드에서 Redis 큐 리스닝을 시작하도록 설정한다.
    *   **신뢰성 큐 패턴 구현**:
        1.  무한 루프 (`while(true)`) 안에서 `redisTemplate.opsForList().move(...)`를 사용하여 `daily_notification:queue:EMAIL`에서 `daily_notification:processing_queue:EMAIL`로 작업을 **원자적으로** 이동시킨다. 이는 `BLMOVE` 명령어에 해당하며, 큐가 비어있을 경우 지정된 시간 동안 대기하여 불필요한 CPU 사용을 방지한다.
        2.  작업 ID를 성공적으로 가져오면, `redisTemplate.opsForHash().entries(...)` (`HGETALL`)를 사용하여 `daily_notification:detail:{uuid}` 키의 Hash에서 알림 상세 정보를 모두 조회한다.
        3.  조회된 정보를 바탕으로 `EmailService` 또는 `SmsService` 등을 호출하여 실제 알림 발송 로직을 수행한다.
        4.  알림 발송이 성공적으로 완료되면, `redisTemplate.opsForList().remove(...)` (`LREM`)를 사용하여 `processing_queue`에서 해당 작업 ID를 **정확히 1개** 제거하여 중복 발송을 방지한다.
    *   **예외 처리**: 알림 발송 중 예외 발생 시, 해당 작업이 `processing_queue`에 그대로 남아있도록 처리하여 작업 유실을 방지한다. 관리자가 문제를 파악하고 조치할 수 있도록 심각한 오류는 로그로 반드시 기록한다.

---

### **Step 4: 최종 검증 및 문서화**

**목표**: 구현된 모든 기능이 `python_ai_server_swagger_documentation.md` 및 `AI 기반 무역 규제 레이더 플랫폼 API 명세서 v6.1`과 완벽히 일치하는지 최종 검증한다.

**주요 작업**:
1.  **API 일치성 검증**: Postman, Insomnia 또는 `curl`을 사용하여 `POST /api/chat` 엔드포인트가 명세서의 SSE 이벤트 흐름과 정확히 일치하는지, 모든 이벤트 타입과 데이터 구조가 올바른지 검증한다.
2.  **데이터 정합성 검증**: 스케줄러와 알림 소비자가 생성/수정하는 데이터가 PostgreSQL DB와 Redis에 각각 정확하게 기록되는지 직접 확인한다.
3.  **구현 계획서 최종 업데이트**: 실제 구현 과정에서 발생한 변경사항, 기술적 결정, 특이사항 등을 본 문서에 최종적으로 반영하여 최신 상태를 유지한다.

---
**문서 종료** 