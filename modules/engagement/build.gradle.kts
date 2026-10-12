// engagement 모듈 — 좋아요(Like) 도메인 (POJO + JPA, Spring Web 의존 없음)
// 다른 모듈에 직접 의존하지 않는다 — 게시물 존재 확인 같은 모듈 간 조립은 앱 계층(customer-api)이 한다.
plugins {
    id("java")
    id("io.spring.dependency-management")
}

apply(plugin = "io.spring.dependency-management")

the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.5.0")
    }
}

dependencies {
    implementation(project(":common"))

    // JPA (Spring Web 없음)
    implementation("jakarta.persistence:jakarta.persistence-api")
    implementation("org.springframework.data:spring-data-jpa")
}
