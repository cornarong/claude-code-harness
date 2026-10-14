package com.echo.moderation.application;

import com.echo.moderation.domain.BannedWordDetectedException;
import com.echo.moderation.infrastructure.BannedWordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 금칙어 필터 애플리케이션 서비스. 콘텐츠에 금칙어가 들어 있는지 검사한다.
 *
 * <p>금칙어 목록은 banned_words 테이블에서 읽는다(하드코딩하지 않음).
 * 게시물 등록 흐름에 끼워 넣는 일은 호출자(앱 계층)가 한다 — 이 모듈은 post 모듈에 의존하지 않는다.
 */
@Service
@Transactional(readOnly = true)
public class BannedWordService {

    private final BannedWordRepository bannedWordRepository;

    public BannedWordService(BannedWordRepository bannedWordRepository) {
        this.bannedWordRepository = bannedWordRepository;
    }

    /** 금칙어가 하나라도 들어 있으면 BannedWordDetectedException. */
    public void validate(String content) {
        boolean detected = bannedWordRepository.findAll().stream()
                .anyMatch(bannedWord -> bannedWord.matches(content));
        if (detected) {
            throw new BannedWordDetectedException();
        }
    }
}
