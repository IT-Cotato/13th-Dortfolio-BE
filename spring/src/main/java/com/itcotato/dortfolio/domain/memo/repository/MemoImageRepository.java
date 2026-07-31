package com.itcotato.dortfolio.domain.memo.repository;

import com.itcotato.dortfolio.domain.memo.entity.MemoImage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoImageRepository extends JpaRepository<MemoImage, UUID> {

	List<MemoImage> findAllByMemo_IdOrderBySortOrderAsc(UUID memoId);

	// 목록 조회 시 N+1 방지를 위해 여러 메모의 이미지를 한 번에 조회
	List<MemoImage> findAllByMemo_IdInOrderBySortOrderAsc(List<UUID> memoIds);

	Optional<MemoImage> findByIdAndMemo_User_Id(UUID id, UUID userId);

	// 메모 하드 삭제 전 FK로 연결된 이미지를 먼저 정리
	void deleteAllByMemo_IdIn(List<UUID> memoIds);
}
