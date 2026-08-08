package com.itcotato.dortfolio.domain.activity.repository;

import com.itcotato.dortfolio.domain.record.entity.Record;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 활동 삭제·복구가 그 안의 기록까지 미치도록 기록을 조회한다.
 *
 * 기록 도메인의 RecordRepository를 고치지 않으려고 활동 쪽에 따로 뒀다.
 * (조회만 하고 기록 엔티티의 공개 메서드만 사용한다)
 */
@Repository
@RequiredArgsConstructor
public class ActivityRecordQueryRepository {

    private final EntityManager entityManager;

    /** 활동에 속한, 아직 삭제되지 않은 기록 */
    public List<Record> findActiveRecords(UUID activityId) {
        return entityManager.createQuery("""
                        select r
                        from Record r
                        where r.activity.id = :activityId
                            and r.deletedAt is null
                        """, Record.class)
                .setParameter("activityId", activityId)
                .getResultList();
    }

    /**
     * 활동과 함께 삭제된 기록.
     *
     * 활동을 지울 때 기록도 바로 뒤이어 지우므로, 기록의 삭제 시각은 활동의 삭제 시각과 같거나 조금 뒤가 된다.
     * 사용자가 그 전에 개별적으로 지운 기록은 삭제 시각이 더 이르기 때문에 복구 대상에서 빠진다.
     */
    public List<Record> findRecordsDeletedWithActivity(UUID activityId, LocalDateTime activityDeletedAt) {
        return entityManager.createQuery("""
                        select r
                        from Record r
                        where r.activity.id = :activityId
                            and r.deletedAt >= :activityDeletedAt
                        """, Record.class)
                .setParameter("activityId", activityId)
                .setParameter("activityDeletedAt", activityDeletedAt)
                .getResultList();
    }
}
