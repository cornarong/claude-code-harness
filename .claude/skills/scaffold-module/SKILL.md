---
name: scaffold-module
description: Echo 프로젝트에 새 도메인 모듈을 신설한다. 모듈 이름을 받아 modules/{module} 아래 헥사고날 골격(domain/application/infrastructure)과 짝 테스트, Gradle 등록, customer-api 연결(Controller·DTO·통합 테스트), 훅 매핑까지 한 번에 잡는다. 사용자가 "새 모듈 만들어줘", "like 모듈 추가", "모듈 골격 잡아줘", "모듈 신설"처럼 modules/ 아래에 새 모듈을 만들려 할 때는 직접 파일을 만들기 전에 반드시 이 스킬을 먼저 쓴다. 기존 모듈에 클래스를 추가하거나 고치는 작업에는 쓰지 않는다.
argument-hint: <module-name> (소문자 한 단어, 예: like)
---

# scaffold-module — 새 모듈 신설

모듈 이름: `$ARGUMENTS`

Echo 는 모듈마다 같은 모양을 반복한다. 이 스킬은 그 모양을 빠짐없이 재현하기 위한 절차다.
한 군데라도 빠지면 "빌드는 되는데 빈이 안 뜬다", "파일을 고쳐도 테스트가 안 돈다" 같은
조용한 실패가 생기므로, 아래 순서를 건너뛰지 말고 끝까지 진행한다.

## 시작 전 확인

1. **이름 정하기** — 아래 세 가지를 먼저 확정한다.
   - `{module}`: 소문자 영문 한 단어 (예: `like`). 패키지 이름이 되므로 하이픈·대문자·숫자 시작은 안 된다.
   - `{Module}`: 클래스 접두어 (예: `Like`).
   - `{modules}`: 테이블·URL 에 쓰는 복수형 (예: `likes`).

   이름이 없거나 규칙에 맞지 않으면 만들기 전에 사용자에게 묻는다.
2. **이미 있는지** — `modules/{module}` 이 있거나 `settings.gradle.kts` 에 이미 등록돼 있으면 멈추고 알린다. 덮어쓰지 않는다.
3. **만들어도 되는지** — 이 프로젝트는 모듈을 임의로 늘리지 않는다(`.claude/rules/architecture.md`). 사용자가 이 모듈을 만들라고 명시한 경우에만 진행한다.
4. **필드** — 사용자가 도메인 필드를 말해 줬으면 그것으로 만든다. 이름만 줬으면 템플릿의 자리표시 필드(`userId`)로 골격을 잡고, 마지막 보고에서 "필드는 자리표시"라고 알린다.

## 절차

순서에는 이유가 있다. Gradle 과 훅 등록이 먼저여야, 그 뒤에 만드는 Java 파일마다
PostToolUse 훅이 해당 모듈 테스트를 실제로 돌릴 수 있다.

### 1. Gradle 등록
- `settings.gradle.kts` 에 `include("modules:{module}")` 추가
- `modules/{module}/build.gradle.kts` 생성 → 템플릿: `references/module-templates.md`

### 2. 훅 매핑
- `.claude/hooks/run-module-tests.sh` 의 모듈 판별 `case` 에 한 줄 추가:
  `*/modules/{module}/*)    task=":modules:{module}:test" ;;`

  이 줄이 없으면 훅이 새 모듈을 "해당 없음"으로 보고 조용히 통과해서, 테스트가 한 번도 돌지 않는다.

### 3. 모듈 골격 — `references/module-templates.md` 를 읽고 그대로 만든다
만드는 순서: domain → domain 테스트 → infrastructure → application

| 계층 | 파일 | 짝 테스트 |
|---|---|---|
| domain | `{Module}.java` (엔티티) | `{Module}Test.java` (같은 패키지, `src/test`) |
| infrastructure | `{Module}Repository.java` | 없음 (인터페이스) |
| application | `{Module}Service.java` | 5단계의 `{Module}IntegrationTest` 가 대신한다 |

패키지는 `com.echo.{module}.{layer}`, 경로는 `modules/{module}/src/main/java/com/echo/{module}/{layer}/`.

### 4. 앱 계층 연결 — `references/app-templates.md` 를 읽고 그대로 만든다
- `apps/customer-api/build.gradle.kts` 에 `implementation(project(":modules:{module}"))` 추가
- `EchoApplication.java` 의 세 목록에 패키지 추가 (하나라도 빠지면 빈·엔티티·리포지토리가 등록되지 않는다)
  - `scanBasePackages` ← `com.echo.{module}.application`
  - `@EntityScan` ← `com.echo.{module}.domain`
  - `@EnableJpaRepositories` ← `com.echo.{module}.infrastructure`

### 5. HTTP 진입점 — `apps/customer-api` 의 `com.echo.api.{module}`
- `{Module}Response.java` (record DTO)
- `{Module}Controller.java`
- `src/test/java/com/echo/api/{Module}IntegrationTest.java`

### 6. 검증
```bash
./gradlew :modules:{module}:test
./gradlew :apps:customer-api:test
./gradlew build
```
그리고 `modules/{module}` 안에 다른 모듈 import 가 없는지 확인한다:
`import com.echo.` 로 검색했을 때 `com.echo.{module}.` 과 `com.echo.common.` 만 나와야 한다.

## Echo 관습 — 골격을 채울 때 지킬 것

- **모듈끼리 직접 의존하지 않는다.** `modules/{module}` 은 다른 모듈(`com.echo.user`, `com.echo.post` 등)을 import 하지 않고, `build.gradle.kts` 에도 `project(":modules:...")` 를 넣지 않는다. 다른 모듈의 대상은 엔티티가 아니라 ID 값(`Long userId`)으로만 가리킨다. 공유할 타입·계약이 꼭 필요하면 `common` 에 두고 그것을 import 한다.
- **도메인 모듈에 Spring Web 을 넣지 않는다.** 모듈 안은 POJO + JPA 만. `@RestController`, `ResponseStatusException`, `spring-boot-starter-web` 은 모듈에 두지 않는다. 그래서 도메인 예외는 HTTP 를 모르는 순수 예외로 만들고, 상태 코드 변환은 Controller 가 한다.
- **Controller 는 `apps/customer-api` 에만 둔다.** 여러 모듈을 엮는 일(예: 유저 존재 확인 후 위임)도 여기서 한다. 이곳이 유일한 조립 지점이다.
- **응답은 `ApiResponse.ok(...)` 로 감싼다.** DTO 는 `record` 로 만들고 엔티티를 그대로 내보내지 않는다.
- **새 클래스에는 짝 테스트를 만든다.** 이름은 `{이름}Test`, HTTP 경로는 `{Module}IntegrationTest`.

전체 규칙의 원본은 `CLAUDE.md` 와 `.claude/rules/architecture.md` 다. 여기 적힌 것과 다르면 원본을 따른다.

## 진행 중 훅이 내는 신호

이 프로젝트의 훅은 스킬 실행 중에도 그대로 동작한다. 아래 두 가지는 예상된 신호이니 당황하지 말고 다음 단계로 해소한다.

- **"⚠️ 짝 테스트 없음"** — 클래스를 만든 직후, 그 테스트를 만들기 전에 뜬다. 바로 다음에 짝 테스트를 만들면 된다. `{Module}Service` 와 `{Module}Controller` 에 대한 경고는 5단계의 `{Module}IntegrationTest` 로 해소된다(Service 용 단위 테스트를 모듈 안에 따로 만들지 않는다 — 모듈에는 목 라이브러리가 없다).
- **"❌ 차단: … 직접 import 할 수 없다"** — 다른 모듈을 import 하려 했다는 뜻이다. 우회하지 말고 ID 값으로 바꾸거나 `common` 을 경유하도록 설계를 고친다. (이 차단 훅은 지금 follow 모듈만 검사한다. 새 모듈에서는 훅이 막아 주지 않으므로 6단계의 import 검색으로 직접 확인한다.)

테스트 실패(`❌ :modules:{module}:test 실패`)는 예상된 신호가 아니다. 원인을 고친 뒤 진행한다.

## 마지막 보고

끝나면 사용자에게 짧게 알린다.

- 만든 파일과 고친 파일 목록
- 6단계 검증 결과 (통과/실패를 그대로)
- 자리표시로 둔 것 (예: `userId` 필드, `create` 유스케이스) — 실제 도메인에 맞게 바꿔야 할 부분
