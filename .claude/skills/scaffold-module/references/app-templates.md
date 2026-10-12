# 앱 계층 템플릿 — `apps/customer-api`

`{module}` / `{Module}` / `{modules}` 를 실제 이름으로 바꿔 쓴다 (예: `like` / `Like` / `likes`).
follow 를 본떠 만든 것이므로, 애매하면 `apps/customer-api/.../api/follow/` 의 실제 파일을 참고한다.

## 목차
1. `build.gradle.kts` 의존성 추가
2. `EchoApplication.java` 스캔 등록
3. `{Module}Response.java`
4. `{Module}Controller.java`
5. `{Module}IntegrationTest.java`

---

## 1. `apps/customer-api/build.gradle.kts`

`dependencies` 의 모듈 목록 끝에 한 줄 추가한다.

```kotlin
    implementation(project(":modules:{module}"))
```

## 2. `src/main/java/com/echo/api/EchoApplication.java`

멀티모듈이라 패키지를 명시적으로 스캔한다. 세 목록 모두에 추가해야 한다 —
하나만 빠져도 컴파일은 되지만 앱이 뜰 때 빈을 찾지 못해 실패한다.

```java
@SpringBootApplication(scanBasePackages = {
    // ...기존 항목
    "com.echo.{module}.application"
})
@EntityScan(basePackages = {
    // ...기존 항목
    "com.echo.{module}.domain"
})
@EnableJpaRepositories(basePackages = {
    // ...기존 항목
    "com.echo.{module}.infrastructure"
})
```

## 3. `src/main/java/com/echo/api/{module}/{Module}Response.java`

DTO 는 record. 엔티티를 그대로 내보내지 않고 `from` 으로 변환한다.

```java
package com.echo.api.{module};

import com.echo.{module}.domain.{Module};
import java.time.LocalDateTime;

/**
 * POST /{modules} 응답 DTO.
 */
public record {Module}Response(Long id, Long userId, LocalDateTime createdAt) {

    public static {Module}Response from({Module} entity) {
        return new {Module}Response(
            entity.getId(),
            entity.getUserId(),
            entity.getCreatedAt()
        );
    }
}
```

## 4. `src/main/java/com/echo/api/{module}/{Module}Controller.java`

여기가 모듈들을 잇는 조립 지점이다. user 모듈로 유저 존재를 확인한 뒤 새 모듈에 위임한다.
도메인 예외 → HTTP 상태 변환도 여기서 한다(모듈은 Spring Web 을 모른다).

```java
package com.echo.api.{module};

import com.echo.common.ApiResponse;
import com.echo.{module}.application.{Module}Service;
import com.echo.{module}.domain.{Module};
import com.echo.user.application.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * {Module} HTTP API 컨트롤러.
 * X-User-Id 헤더로 요청자를 식별한다 (인증은 15장에서 도입).
 *
 * <p>user와 {module} 두 모듈을 잇는 조립 지점(composition root)이다. 두 모듈은 서로 직접 의존하지 않는다.
 */
@RestController
@RequestMapping("/{modules}")
@Tag(name = "{Module}", description = "{Module} API")
public class {Module}Controller {

    private final UserService userService;
    private final {Module}Service {module}Service;

    public {Module}Controller(UserService userService, {Module}Service {module}Service) {
        this.userService = userService;
        this.{module}Service = {module}Service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "생성", description = "없는 유저면 404.")
    public ApiResponse<{Module}Response> create(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {

        requireUser(userId);

        // 도메인/application은 Spring Web에 의존하지 않으므로, 도메인 예외의 HTTP 상태 변환은 컨트롤러에서 처리한다.
        try {
            {Module} entity = {module}Service.create(userId);
            return ApiResponse.ok({Module}Response.from(entity), "생성했습니다.");
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private void requireUser(Long id) {
        if (!userService.exists(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다. id=" + id);
        }
    }
}
```

## 5. `src/test/java/com/echo/api/{Module}IntegrationTest.java`

패키지는 `com.echo.api` (하위 패키지가 아니다 — 기존 통합 테스트와 같은 자리).
Service 와 Controller 의 짝 테스트 역할을 함께 한다.

```java
package com.echo.api;

import com.echo.user.domain.User;
import com.echo.user.infrastructure.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /{modules} 통합 테스트.
 * user → {module} → HTTP 조립 경로가 모듈 간 직접 의존 없이 앱 계층에서 연결되는지 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class {Module}IntegrationTest {

    private static final long UNKNOWN_USER_ID = 999999L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("POST /{modules} — 정상 생성 시 201 반환")
    void create_success() throws Exception {
        Long me = createUser();

        mockMvc.perform(post("/{modules}").header("X-User-Id", me))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(me));
    }

    @Test
    @DisplayName("POST /{modules} — 요청자(X-User-Id)가 없는 유저면 404 반환")
    void create_userNotFound_404() throws Exception {
        mockMvc.perform(post("/{modules}").header("X-User-Id", UNKNOWN_USER_ID))
                .andExpect(status().isNotFound());
    }

    /** 유저를 저장하고 생성된 id를 반환한다. username은 유니크 제약이 있어 매번 새로 만든다. */
    private Long createUser() {
        String username = "user-" + UUID.randomUUID();
        return userRepository.save(new User(username, username)).getId();
    }
}
```
