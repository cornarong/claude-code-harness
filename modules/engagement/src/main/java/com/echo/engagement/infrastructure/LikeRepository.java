package com.echo.engagement.infrastructure;

import com.echo.engagement.domain.Like;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Like JPA 리포지토리. infrastructure 계층.
 */
public interface LikeRepository extends JpaRepository<Like, Long> {

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    /** 좋아요 삭제. 삭제된 행 수를 반환한다. */
    long deleteByUserIdAndPostId(Long userId, Long postId);

    /** 게시물이 받은 좋아요 수. */
    long countByPostId(Long postId);
}
