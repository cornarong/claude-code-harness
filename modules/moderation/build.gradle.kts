// moderation 모듈 — 콘텐츠 안전 관리: 금칙어(BannedWord)·신고(Report) 도메인 (POJO + JPA, Spring Web 의존 없음)
// 다른 모듈에 직접 의존하지 않는다 — 게시물 등록 시 금칙어 검사 같은 모듈 간 조립은 앱 계층(customer-api)이 한다.
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
