package com.itcotato.dortfolio.domain.user.repository;

import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.entity.UserTermAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface UserTermAgreementRepository extends JpaRepository<UserTermAgreement, UUID> {

    @Modifying
    @Query("DELETE FROM UserTermAgreement uta WHERE uta.user = :user")
    void deleteAllByUser(@Param("user") User user);
}
