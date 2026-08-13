package com.itcotato.dortfolio.domain.memo.repository;

import com.itcotato.dortfolio.domain.memo.entity.Memo;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemoRepository extends JpaRepository<Memo, UUID> {

	List<Memo> findAllByIdInAndUser_IdAndDeletedAtIsNull(List<UUID> ids, UUID userId);

	List<Memo> findAllByIdInAndUser_Id(List<UUID> ids, UUID userId);

	Optional<Memo> findByIdAndUser_Id(UUID id, UUID userId);

	// 기능명세서 3.4: 기간(생성일) 내 메모 조회
	List<Memo> findAllByUser_IdAndDeletedAtIsNullAndCreatedAtBetweenOrderByCreatedAtDesc(
			UUID userId, LocalDateTime from, LocalDateTime to);

	// 기능명세서 3.4: 활동 태그별 필터링 + 기간 조회
	List<Memo> findAllByUser_IdAndActivity_IdAndDeletedAtIsNullAndCreatedAtBetweenOrderByCreatedAtDesc(
			UUID userId, UUID activityId, LocalDateTime from, LocalDateTime to);

	// 기능명세서 3. 메모하기: 생성일로부터 30일 지나면 자동 삭제.
	// 사용자가 이미 지운 메모는 제외한다. 만료가 임박한 메모를 삭제한 직후 이 작업이 돌면
	// 유예 기간을 건너뛰고 지워져 실행취소가 불가능해진다
	List<Memo> findAllByExpiresAtBeforeAndDeletePendingUntilIsNull(LocalDateTime dateTime);

	// 실행취소 유예가 끝난 메모. 스케줄러가 실제로 지운다
	List<Memo> findAllByDeletePendingUntilBefore(LocalDateTime dateTime);

	// 기록에 연결된 메모는 삭제할 수 없다(record_memos.memo_id가 필수 FK).
	// RecordMemoRepository를 주입하면 memo -> record 서비스 의존이 생겨 조회 쿼리로만 확인한다.
	@Query("select distinct rm.memo.id from RecordMemo rm where rm.memo.id in :memoIds")
	List<UUID> findIdsLinkedToRecords(@Param("memoIds") List<UUID> memoIds);
}
