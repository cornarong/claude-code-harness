package com.echo.engagement.application;

import com.echo.engagement.domain.AlreadyLikedException;
import com.echo.engagement.domain.Like;
import com.echo.engagement.infrastructure.LikeRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Like 도메인 애플리케이션 서비스. 좋아요·좋아요 취소·좋아요 수 조회 유스케이스.
 *
 * <p>게시물 존재 여부는 이 모듈이 알 수 없다(post 모듈에 의존하지 않음). 호출자(앱 계층)가 먼저 확인한다.
 */
@Service
@Transactional(readOnly = true)
public class LikeService {

    private final LikeRepository likeRepository;

    public LikeService(LikeRepository likeRepository) {
        this.likeRepository = likeRepository;
    }

    @Transactional
    public Like like(Long userId, Long postId) {
        Like like = new Like(userId, postId);
        if (likeRepository.existsByUserIdAndPostId(userId, postId)) {
            throw new AlreadyLikedException(userId, postId);
        }
        try {
            return likeRepository.saveAndFlush(like);
        } catch (DataIntegrityViolationException e) {
            // 동시 요청이 존재 확인을 함께 통과한 경우 — 유니크 제약이 최종 방어선
            throw new AlreadyLikedException(userId, postId);
        }
    }

    /** 좋아요 취소. 좋아요를 누르지 않았어도 예외 없이 끝난다(멱등). */
    @Transactional
    public void unlike(Long userId, Long postId) {
        likeRepository.deleteByUserIdAndPostId(userId, postId);
    }

    /** 게시물이 받은 좋아요 수. */
    public long countByPost(Long postId) {
        return likeRepository.countByPostId(postId);
    }
}
