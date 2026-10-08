package com.echo.follow.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Follow 도메인 엔티티. follower가 followee를 팔로우하는 관계 한 건.
 *
 * <p>user 모듈을 import하지 않고 유저 id만 보관한다.
 */
@Entity
@Table(
    name = "follows",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_follows_follower_followee",
        columnNames = {"followerId", "followeeId"}
    )
)
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long followerId;

    @Column(nullable = false, updatable = false)
    private Long followeeId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Follow() {
    }

    public Follow(Long followerId, Long followeeId) {
        if (followerId == null || followeeId == null) {
            throw new IllegalArgumentException("followerId와 followeeId는 비어 있을 수 없습니다.");
        }
        if (followerId.equals(followeeId)) {
            throw new IllegalArgumentException("자기 자신은 팔로우할 수 없습니다.");
        }
        this.followerId = followerId;
        this.followeeId = followeeId;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getFollowerId() { return followerId; }
    public Long getFolloweeId() { return followeeId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
