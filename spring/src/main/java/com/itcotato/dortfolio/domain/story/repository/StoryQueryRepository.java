package com.itcotato.dortfolio.domain.story.repository;

import com.itcotato.dortfolio.domain.activity.entity.Activity;
import com.itcotato.dortfolio.domain.activity.entity.ActivityStatus;
import com.itcotato.dortfolio.domain.record.entity.Record;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

// 나의 스토리(활동 보관함) 타임라인 전용 조회.
// 기록 조회는 record 도메인 소유라 RecordRepository를 수정하지 않고 이 클래스에서 직접 조회한다.
@Repository
public class StoryQueryRepository {

	private final EntityManager entityManager;

	public StoryQueryRepository(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	// 기능명세서 5.1: 보관된 활동을 활동 시간순으로 조회 (활동 종류는 fetch join으로 N+1 방지)
	public List<Activity> findArchivedActivities(UUID userId) {
		return entityManager.createQuery("""
						select a from Activity a
						join fetch a.activityType
						where a.user.id = :userId
							and a.status = :status
							and a.deletedAt is null
						order by a.startedAt asc, a.id asc
						""", Activity.class)
				.setParameter("userId", userId)
				.setParameter("status", ActivityStatus.ARCHIVED)
				.getResultList();
	}

	// 기능명세서 5.1.1: 활동에 속한 기록을 작성순으로 조회 (활동별 개별 조회 시 N+1이라 한 번에 조회)
	public List<Record> findRecordsByActivityIds(List<UUID> activityIds) {
		if (activityIds.isEmpty()) {
			return List.of();
		}

		return entityManager.createQuery("""
						select r from Record r
						where r.activity.id in :activityIds
							and r.deletedAt is null
						order by r.createdAt asc, r.id asc
						""", Record.class)
				.setParameter("activityIds", activityIds)
				.getResultList();
	}
}
