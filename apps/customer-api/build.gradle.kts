// customer-api — 일반 사용자 HTTP API 진입점
plugins {
    id("java")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

// Docker Engine 29+ 는 구버전 API 클라이언트를 거부한다 — Boot 3.5.0 기본값(1.21.0)보다 높여서 고정
extra["testcontainers.version"] = "1.21.4"

dependencies {
    implementation(project(":common"))
    implementation(project(":modules:user"))
    implementation(project(":modules:post"))
    implementation(project(":modules:timeline"))
    implementation(project(":modules:follow"))
    implementation(project(":modules:engagement"))

    // Spring Boot Web + JPA + Validation + Actuator
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // PostgreSQL + Flyway 마이그레이션
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    // Swagger UI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")

    // 테스트 — 통합 테스트는 Testcontainers로 일회용 PostgreSQL을 띄운다 (Docker 필요)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:postgresql")
}

// bootRun 은 프로젝트 루트의 .env(git 추적 제외)를 읽어 환경변수로 넘긴다.
// 이미 셸에 설정된 환경변수가 있으면 그 값을 우선한다.
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        envFile.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
            .forEach { line ->
                val key = line.substringBefore("=").trim()
                if (System.getenv(key) == null) {
                    environment(key, line.substringAfter("=").trim())
                }
            }
    }
}
