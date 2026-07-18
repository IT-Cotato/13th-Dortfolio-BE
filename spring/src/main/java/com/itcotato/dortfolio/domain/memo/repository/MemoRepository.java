package com.itcotato.dortfolio.domain.memo.repository;

import com.itcotato.dortfolio.domain.memo.entity.Memo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoRepository extends JpaRepository<Memo, UUID> {

	List<Memo> findAllByIdInAndUser_IdAndDeletedAtIsNull(List<UUID> ids, UUID userId);

	List<Memo> findAllByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId);

	Optional<Memo> findByIdAndUser_Id(UUID id, UUID userId);
}
