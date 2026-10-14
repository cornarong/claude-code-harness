package com.echo.moderation.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * BannedWord 도메인 엔티티. 게시물에 쓸 수 없는 금칙어 한 개.
 *
 * <p>목록은 코드가 아니라 DB(banned_words)로 관리한다. 대소문자를 구분하지 않도록 소문자로 정규화해 보관한다.
 */
@Entity
@Table(
    name = "banned_words",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_banned_words_word",
        columnNames = {"word"}
    )
)
public class BannedWord {

    public static final int MAX_LENGTH = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false, length = MAX_LENGTH)
    private String word;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected BannedWord() {
    }

    public BannedWord(String word) {
        if (word == null || word.isBlank()) {
            throw new IllegalArgumentException("금칙어는 비어 있을 수 없습니다.");
        }
        String normalized = normalize(word);
        if (normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("금칙어는 " + MAX_LENGTH + "자를 초과할 수 없습니다.");
        }
        this.word = normalized;
        this.createdAt = LocalDateTime.now();
    }

    /** 콘텐츠에 이 금칙어가 들어 있는지 (대소문자 무시, 부분 일치). */
    public boolean matches(String content) {
        return content != null && normalize(content).contains(word);
    }

    private static String normalize(String text) {
        return text.strip().toLowerCase(Locale.ROOT);
    }

    public Long getId() { return id; }
    public String getWord() { return word; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
