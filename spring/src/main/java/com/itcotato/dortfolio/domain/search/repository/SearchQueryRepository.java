package com.itcotato.dortfolio.domain.search.repository;

import com.itcotato.dortfolio.domain.record.entity.Record;
import com.itcotato.dortfolio.domain.record.entity.RecordAnswer;
import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

// 기능명세서 5.3 활동 검색 전용 조회.
// 기록 조회는 record 도메인 소유라 RecordRepository를 수정하지 않고 여기서 직접 조회한다.
@Repository
public class SearchQueryRepository {

	// RecordAnswer.answerText가 @Lob(CLOB)이라 JPQL에서 lower()를 쓸 수 없어 답변 매칭만 네이티브 쿼리로 처리한다.
	// (cast(... as String)은 varchar 길이 제한으로 잘릴 수 있어 사용하지 않음)
	private static final String ANSWER_MATCH_SQL = """
			select distinct ra.record_id
			from record_answers ra
			join records r on r.id = ra.record_id
			where r.user_id = :userId
				and lower(ra.answer_text) like :keyword escape '\\'
			""";

	private static final String BASE_CONDITION = """
			r.user.id = :userId
				and r.deletedAt is null
				and r.activity.deletedAt is null
				and r.template.deletedAt is null
			""";

	private final EntityManager entityManager;

	public SearchQueryRepository(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	/* 기록 내용(답변)에 키워드가 포함된 기록 ID 목록 */
	public List<UUID> findRecordIdsWithMatchingAnswer(UUID userId, String keyword) {
		return entityManager.createNativeQuery(ANSWER_MATCH_SQL, UUID.class)
				.setParameter("userId", userId)
				.setParameter("keyword", keyword)
				.getResultList();
	}

	public long countMatchingRecords(UUID userId, String keyword, Collection<UUID> answerMatchedIds) {
		var query = entityManager.createQuery(
						"select count(r) from Record r where " + matchCondition(answerMatchedIds), Long.class)
				.setParameter("userId", userId)
				.setParameter("keyword", keyword);

		bindAnswerMatchedIds(query, answerMatchedIds);
		return query.getSingleResult();
	}

	// 활동/템플릿은 단일 연관이라 fetch join과 페이징을 함께 써도 메모리 페이징이 발생하지 않는다
	public List<Record> findMatchingRecords(
			UUID userId,
			String keyword,
			Collection<UUID> answerMatchedIds,
			int page,
			int size
	) {
		var query = entityManager.createQuery("""
								select r from Record r
								join fetch r.activity a
								join fetch a.activityType
								join fetch r.template
								where
								""" + matchCondition(answerMatchedIds) + """
								order by r.createdAt desc, r.id desc
								""", Record.class)
				.setParameter("userId", userId)
				.setParameter("keyword", keyword)
				.setFirstResult(page * size)
				.setMaxResults(size);

		bindAnswerMatchedIds(query, answerMatchedIds);
		return query.getResultList();
	}

	// 검색 결과에 보여줄 기록 내용을 고르기 위해 해당 기록들의 답변을 한 번에 조회한다
	public List<RecordAnswer> findAnswersByRecordIds(List<UUID> recordIds) {
		if (recordIds.isEmpty()) {
			return List.of();
		}

		return entityManager.createQuery("""
						select a from RecordAnswer a
						where a.record.id in :recordIds
						order by a.sortOrder asc
						""", RecordAnswer.class)
				.setParameter("recordIds", recordIds)
				.getResultList();
	}

	// 답변 매칭 결과가 없으면 in 절을 아예 넣지 않는다 (JPQL은 빈 in 절을 허용하지 않음)
	private String matchCondition(Collection<UUID> answerMatchedIds) {
		String titleCondition = "lower(r.title) like :keyword escape '\\'";
		String condition = answerMatchedIds.isEmpty()
				? titleCondition
				: titleCondition + " or r.id in :answerMatchedIds";

		return BASE_CONDITION + " and (" + condition + ")";
	}

	private void bindAnswerMatchedIds(jakarta.persistence.Query query, Collection<UUID> answerMatchedIds) {
		if (!answerMatchedIds.isEmpty()) {
			query.setParameter("answerMatchedIds", answerMatchedIds);
		}
	}
}
