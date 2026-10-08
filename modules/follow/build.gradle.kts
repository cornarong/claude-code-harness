// follow 모듈 — Follow 도메인 (POJO + JPA, Spring Web 의존 없음)
// 유저 존재 확인은 앱 계층(customer-api)이 UserService로 수행한다 — user 모듈에 직접 의존하지 않는다.
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
