package com.echo.engagement.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Like 도메인 엔티티. 유저가 게시물에 누른 좋아요 한 건.
 *
 * <p>user·post 모듈을 import하지 않고 id만 보관한다.
 */
@Entity
@Table(
    name = "likes",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_likes_user_post",
        columnNames = {"userId", "postId"}
    )
)
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false, updatable = false)
    private Long postId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Like() {
    }

    public Like(Long userId, Long postId) {
        if (userId == null || postId == null) {
            throw new IllegalArgumentException("userId와 postId는 비어 있을 수 없습니다.");
        }
        this.userId = userId;
        this.postId = postId;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getPostId() { return postId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
