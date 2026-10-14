package com.echo.moderation.infrastructure;

import com.echo.moderation.domain.BannedWord;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * BannedWord JPA 리포지토리. infrastructure 계층.
 */
public interface BannedWordRepository extends JpaRepository<BannedWord, Long> {
}
