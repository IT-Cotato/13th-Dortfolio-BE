package com.itcotato.dortfolio.domain.memo.repository;

import com.itcotato.dortfolio.domain.memo.entity.Memo;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoRepository extends JpaRepository<Memo, UUID> {

	List<Memo> findAllByIdInAndUser_IdAndDeletedAtIsNull(List<UUID> ids, UUID userId);
}
