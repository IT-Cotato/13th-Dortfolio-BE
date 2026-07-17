package com.itcotato.dortfolio.domain.user.repository;

import com.itcotato.dortfolio.domain.user.entity.UserTermAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface UserTermAgreementRepository extends JpaRepository<UserTermAgreement, UUID> {
}
