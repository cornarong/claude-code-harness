package com.echo.moderation.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * BannedWord 도메인 불변식 단위 테스트 — Spring 컨텍스트 없이 순수 POJO로 검증.
 */
class BannedWordTest {

    @Test
    @DisplayName("금칙어가 null이거나 공백이면 IllegalArgumentException")
    void blankWord_throws() {
        assertThrows(IllegalArgumentException.class, () -> new BannedWord(null));
        assertThrows(IllegalArgumentException.class, () -> new BannedWord("   "));
    }

    @Test
    @DisplayName("50자를 초과하면 IllegalArgumentException")
    void tooLongWord_throws() {
        assertThrows(IllegalArgumentException.class, () -> new BannedWord("a".repeat(51)));
    }

    @Test
    @DisplayName("앞뒤 공백을 지우고 소문자로 정규화해 보관한다")
    void word_isNormalized() {
        BannedWord bannedWord = new BannedWord("  SpAm ");

        assertEquals("spam", bannedWord.getWord());
        assertNotNull(bannedWord.getCreatedAt());
    }

    @Test
    @DisplayName("콘텐츠에 금칙어가 들어 있으면 대소문자와 상관없이 일치한다")
    void matches_containsIgnoringCase() {
        BannedWord bannedWord = new BannedWord("spam");

        assertTrue(bannedWord.matches("이건 SPAM 광고입니다"));
        assertTrue(bannedWord.matches("spammer"));
        assertFalse(bannedWord.matches("평범한 글"));
        assertFalse(bannedWord.matches(null));
    }
}
