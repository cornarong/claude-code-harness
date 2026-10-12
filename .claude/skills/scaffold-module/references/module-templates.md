# 모듈 템플릿 — `modules/{module}`

`{module}` / `{Module}` / `{modules}` 를 실제 이름으로 바꿔 쓴다 (예: `like` / `Like` / `likes`).
follow 모듈을 본떠 만든 것이므로, 애매하면 `modules/follow` 의 실제 파일을 참고한다.

## 목차
1. `build.gradle.kts`
2. domain — `{Module}.java`
3. domain 테스트 — `{Module}Test.java`
4. infrastructure — `{Module}Repository.java`
5. application — `{Module}Service.java`
6. (필요할 때만) 도메인 예외

---

## 1. `modules/{module}/build.gradle.kts`

의존성은 `common` 과 JPA 뿐이다. `spring-boot-starter-web` 이나 `project(":modules:...")` 를 추가하지 않는다.
JUnit 은 루트 `build.gradle.kts` 가 모든 서브프로젝트에 넣어 주므로 여기 적지 않는다.

```kotlin
// {module} 모듈 — {Module} 도메인 (POJO + JPA, Spring Web 의존 없음)
// 다른 모듈에 직접 의존하지 않는다 — 모듈 간 조립은 앱 계층(customer-api)이 한다.
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
```

## 2. `src/main/java/com/echo/{module}/domain/{Module}.java`

불변식은 생성자에서 검증한다. 다른 모듈의 대상은 엔티티가 아니라 ID 로만 보관한다.
`userId` 는 자리표시 필드다 — 사용자가 필드를 알려 줬으면 그것으로 바꾼다.

```java
package com.echo.{module}.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * {Module} 도메인 엔티티.
 *
 * <p>user 모듈을 import하지 않고 유저 id만 보관한다.
 */
@Entity
@Table(name = "{modules}")
public class {Module} {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected {Module}() {
    }

    public {Module}(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId는 비어 있을 수 없습니다.");
        }
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
```

## 3. `src/test/java/com/echo/{module}/domain/{Module}Test.java`

Spring 컨텍스트 없이 순수 POJO 로 불변식을 검증한다. 불변식을 추가하면 테스트도 같이 늘린다.

```java
package com.echo.{module}.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {Module} 도메인 불변식 단위 테스트 — Spring 컨텍스트 없이 순수 POJO로 검증.
 */
class {Module}Test {

    @Test
    @DisplayName("userId가 null이면 IllegalArgumentException")
    void nullUserId_throws() {
        assertThrows(IllegalArgumentException.class, () -> new {Module}(null));
    }

    @Test
    @DisplayName("유효한 값은 정상 생성되고 createdAt이 채워진다")
    void valid_constructsSuccessfully() {
        {Module} entity = new {Module}(1L);

        assertEquals(1L, entity.getUserId());
        assertNotNull(entity.getCreatedAt());
    }
}
```

## 4. `src/main/java/com/echo/{module}/infrastructure/{Module}Repository.java`

```java
package com.echo.{module}.infrastructure;

import com.echo.{module}.domain.{Module};
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * {Module} JPA 리포지토리. infrastructure 계층.
 */
public interface {Module}Repository extends JpaRepository<{Module}, Long> {
}
```

## 5. `src/main/java/com/echo/{module}/application/{Module}Service.java`

읽기 전용을 기본으로 두고 쓰기 메서드에만 `@Transactional` 을 붙인다.
`create` 는 자리표시 유스케이스다 — 실제 유스케이스 이름으로 바꾼다.
다른 모듈에 물어봐야 하는 일(예: 유저가 존재하는가)은 여기서 하지 않고 호출자(Controller)에 맡긴다.

```java
package com.echo.{module}.application;

import com.echo.{module}.domain.{Module};
import com.echo.{module}.infrastructure.{Module}Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {Module} 도메인 애플리케이션 서비스.
 *
 * <p>유저 존재 여부는 이 모듈이 알 수 없다(user 모듈에 의존하지 않음). 호출자(앱 계층)가 먼저 확인한다.
 */
@Service
@Transactional(readOnly = true)
public class {Module}Service {

    private final {Module}Repository {module}Repository;

    public {Module}Service({Module}Repository {module}Repository) {
        this.{module}Repository = {module}Repository;
    }

    @Transactional
    public {Module} create(Long userId) {
        return {module}Repository.save(new {Module}(userId));
    }
}
```

## 6. (필요할 때만) 도메인 예외

"이미 존재함" 같은 도메인 규칙 위반이 있을 때만 만든다. HTTP 를 모르는 순수 예외로 두고,
상태 코드 변환은 Controller 가 한다. 예시는 `modules/follow/.../domain/AlreadyFollowingException.java`.

```java
package com.echo.{module}.domain;

/**
 * (어떤 규칙을 어겼는지 한 줄로)
 */
public class {Rule}Exception extends RuntimeException {

    public {Rule}Exception(String message) {
        super(message);
    }
}
```
