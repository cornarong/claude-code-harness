package com.echo.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * 통합 테스트용 일회용 PostgreSQL 컨테이너.
 * 운영과 같은 PostgreSQL 16 + Flyway 마이그레이션 위에서 테스트한다 (Docker 필요).
 * {@code @ServiceConnection}이 datasource 접속 정보를 컨테이너 것으로 바꿔 준다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:16-alpine");
    }
}
