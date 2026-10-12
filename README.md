# Claude Code Harness Engineering

Claude Code의 하네스(CLAUDE.md, Rules, Hooks, Skills, MCP, Subagents)를 구성/적용하는 실습 프로젝트

프로젝트 대상 : Echo — Threads류 텍스트 SNS 백엔드

## Tech Stack

Java 25 · Spring Boot 3.5.x · Gradle 멀티프로젝트 · PostgreSQL 16(Docker) · Flyway · JUnit 5

## Local Setup

DB : Docker 기반 PostgreSQL 16 · 스키마 : 앱 기동 시 Flyway 자동 적용

1. 프로젝트 루트에 `.env` 파일 생성 (git 추적 제외 · 비밀번호 보관 위치)

   ```dotenv
   POSTGRES_DB=echo
   POSTGRES_USER=echo
   POSTGRES_PASSWORD=<비밀번호>
   POSTGRES_PORT=5432
   ```

2. DB 기동 후 앱 실행

   ```bash
   docker compose up -d
   ./gradlew :apps:customer-api:bootRun   # 루트 .env 를 환경변수로 전달
   ```

참고

- IDE 직접 실행 : 실행 구성에 `POSTGRES_PASSWORD` 환경변수 추가 필요
- 테스트(`./gradlew test`) : Testcontainers 기반 일회용 PostgreSQL 사용 · `.env` 불필요 · Docker 실행 필수
