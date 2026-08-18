package com.itcotato.dortfolio.domain.user.service;

import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import com.itcotato.dortfolio.global.exception.types.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SignUpEmailConflictResolver {

    private final UserRepository userRepository;

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public UserErrorCode resolve(String email) {
        return userRepository.findByEmail(email)
                .filter(user -> "GOOGLE".equalsIgnoreCase(user.getProvider()))
                .map(user -> UserErrorCode.GOOGLE_ACCOUNT_ALREADY_EXISTS)
                .orElse(UserErrorCode.EMAIL_ALREADY_EXISTS);
    }
}
