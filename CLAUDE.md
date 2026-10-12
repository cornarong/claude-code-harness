# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

응답은 한국어로 작성한다.

## 프로젝트

Echo — Threads류 텍스트 SNS 백엔드.

## 기술 스택

Java 25 · Spring Boot 3.5.x · Gradle 멀티프로젝트 · PostgreSQL 16(Docker) · Flyway · JUnit 5

## 멀티모듈 구조

- `apps/customer-api` — HTTP 진입점(유일한 Spring Boot 실행 단위)
- `modules/post` — Post 도메인
- `modules/user` — User 도메인
- `common` — 표준 응답/오류 record

## 핵심 규칙

- **의존성 방향**: `apps` → `modules`, `common` / `modules` → `common`. **모듈 간 직접 의존 금지.**
- **각 모듈은 hexagonal**: `domain` / `application` / `infrastructure` 계층.
- **도메인 모듈은 Spring Web 의존 없음** — POJO + JPA만.
- **DTO는 record**.
- **패키지 규칙**: `com.echo.{module}.{layer}`.

## 빌드 / 실행 / 테스트

```bash
./gradlew build                       # 전체 빌드
docker compose up -d                  # PostgreSQL 16 기동 (루트 .env 필요 — README "Local Setup" 참고)
./gradlew :apps:customer-api:bootRun  # 앱 실행 (http://localhost:8080, 루트 .env를 읽는다)
./gradlew test                        # 전체 테스트 (통합 테스트는 Testcontainers — Docker 실행 중이어야 함)
./gradlew :apps:customer-api:test --tests "com.echo.api.PostIntegrationTest"  # 단일 테스트
```

## DB / 마이그레이션

- 스키마는 **Flyway**가 관리한다 — `apps/customer-api/src/main/resources/db/migration/V{n}__*.sql`. Hibernate는 `ddl-auto: validate`.
- 엔티티(테이블·컬럼)를 바꾸면 **새 버전 스크립트를 추가**한다. 이미 적용된 스크립트는 고치지 않는다.
- 접속 정보는 환경변수(`POSTGRES_*`)로만 받는다. 비밀번호를 `application.yml`·`docker-compose.yml`에 적지 않는다.
