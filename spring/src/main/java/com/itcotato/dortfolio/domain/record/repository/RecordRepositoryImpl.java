package com.itcotato.dortfolio.domain.record.repository;

import static com.itcotato.dortfolio.domain.record.entity.QRecord.record;

import com.itcotato.dortfolio.domain.record.dto.req.RecordSearchCondition;
import com.itcotato.dortfolio.domain.record.entity.Record;
import com.itcotato.dortfolio.domain.record.entity.RecordStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;

public class RecordRepositoryImpl implements RecordRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	public RecordRepositoryImpl(EntityManager entityManager) {
		this.queryFactory = new JPAQueryFactory(entityManager);
	}

	@Override
	public List<Record> searchRecords(UUID userId, RecordSearchCondition condition) {
		return baseSearchQuery(userId, condition)
			.orderBy(record.createdAt.desc(), record.updatedAt.desc())
			.fetch();
	}

	@Override
	public List<Record> searchRecords(UUID userId, RecordSearchCondition condition, int page, int size) {
		return baseSearchQuery(userId, condition)
			.orderBy(record.createdAt.desc(), record.updatedAt.desc())
			.offset((long) page * size)
			.limit(size)
			.fetch();
	}

	@Override
	public long countRecords(UUID userId, RecordSearchCondition condition) {
		Long count = queryFactory
			.select(record.count())
			.from(record)
			.where(
				record.user.id.eq(userId),
				record.deletedAt.isNull(),
				record.activity.deletedAt.isNull(),
				record.template.deletedAt.isNull(),
				activityIdEq(condition.activityId()),
				templateIdEq(condition.templateId()),
				statusEq(condition.status())
			)
			.fetchOne();
		return count == null ? 0 : count;
	}

	@Override
	public List<Record> findRecentRecords(UUID userId, int limit) {
		return queryFactory
			.selectFrom(record)
			.join(record.activity).fetchJoin()
			.join(record.template).fetchJoin()
			.where(
				record.user.id.eq(userId),
				record.deletedAt.isNull(),
				record.activity.deletedAt.isNull(),
				record.template.deletedAt.isNull()
			)
			.orderBy(record.createdAt.desc(), record.updatedAt.desc())
			.limit(limit)
			.fetch();
	}

	private JPAQuery<Record> baseSearchQuery(UUID userId, RecordSearchCondition condition) {
		return queryFactory
			.selectFrom(record)
			.join(record.activity).fetchJoin()
			.join(record.template).fetchJoin()
			.where(
				record.user.id.eq(userId),
				record.deletedAt.isNull(),
				record.activity.deletedAt.isNull(),
				record.template.deletedAt.isNull(),
				activityIdEq(condition.activityId()),
				templateIdEq(condition.templateId()),
				statusEq(condition.status())
			);
	}

	private BooleanExpression activityIdEq(UUID activityId) {
		return activityId == null ? null : record.activity.id.eq(activityId);
	}

	private BooleanExpression templateIdEq(UUID templateId) {
		return templateId == null ? null : record.template.id.eq(templateId);
	}

	private BooleanExpression statusEq(RecordStatus status) {
		return status == null ? null : record.status.eq(status);
	}
}
