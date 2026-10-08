package com.echo.user.application;

import com.echo.user.infrastructure.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User 도메인 애플리케이션 서비스.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** 유저 존재 여부. 없을 때의 HTTP 처리(404 등)는 호출자(컨트롤러) 몫이다. */
    public boolean exists(Long id) {
        return userRepository.existsById(id);
    }
}
