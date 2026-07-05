package com.itcotato.dortfolio.domain.user.repository;

import com.itcotato.dortfolio.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email); //이메일 중복 체크용
    Optional<User> findByEmail(String email); // 로그인용
}
