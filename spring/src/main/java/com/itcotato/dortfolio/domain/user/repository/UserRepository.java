package com.itcotato.dortfolio.domain.user.repository;

import com.itcotato.dortfolio.domain.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email); //이메일 중복 체크용
    Optional<User> findByEmail(String email); // 로그인용
    Optional<User> findByProviderAndProviderId(String provider, String providerId);

}
