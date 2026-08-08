package com.itcotato.dortfolio.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationTest {
	private static final String LEGACY_TEMPLATE_ID = "00000000-0000-0000-0000-000000000004";
	private static final String CURRENT_QUESTION_ID = "00000000-0000-0000-0000-000000000010";
	private static final String DELETED_QUESTION_ID = "00000000-0000-0000-0000-000000000011";
	private static final String ORPHAN_QUESTION_ID = "00000000-0000-0000-0000-000000000012";
	private static final String ORPHAN_ANSWER_ID = "00000000-0000-0000-0000-000000000031";
	private static final String UPDATED_ANSWER_ID = "00000000-0000-0000-0000-000000000032";
	private static final String DELETED_ANSWER_ID = "00000000-0000-0000-0000-000000000033";

	@Container
	private final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
		DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres")
	);

	@Test
	void migratesFreshSchemaThroughLatestVersion() {
		MigrateResult result = flyway().migrate();

		assertThat(result.migrationsExecuted).isEqualTo(8);
		assertThat(result.targetSchemaVersion).isEqualTo("8");
		assertMatchingIndexesCreated();
		assertRunningInsightStatusAllowed();
		assertRecordAnswersNormalized();
	}

	@Test
	void migratesLegacyRecordAnswerSnapshotsFromV7() {
		Flyway v7Flyway = flyway(MigrationVersion.fromVersion("7"));
		v7Flyway.migrate();
		seedLegacyRecordAnswers();

		MigrateResult result = flyway().migrate();

		assertThat(result.migrationsExecuted).isEqualTo(1);
		assertThat(result.targetSchemaVersion).isEqualTo("8");
		assertHistoryAnswer(
			ORPHAN_ANSWER_ID,
			ORPHAN_QUESTION_ID,
			"사라진 질문",
			"사라진 설명",
			true,
			1,
			true
		);
		assertHistoryAnswer(
			UPDATED_ANSWER_ID,
			CURRENT_QUESTION_ID,
			"이전 질문",
			"이전 설명",
			true,
			1,
			true
		);
		assertHistoryAnswer(
			DELETED_ANSWER_ID,
			DELETED_QUESTION_ID,
			"삭제 질문",
			"삭제 설명",
			true,
			3,
			false
		);
		assertLegacyTemplateQuestionsRemainSeparated();
		assertRecordAnswersNormalized();
	}

	private Flyway flyway() {
		return flyway(null);
	}

	private Flyway flyway(MigrationVersion target) {
		var configuration = Flyway.configure()
			.dataSource(dataSource())
			.locations("classpath:db/migration")
			.baselineOnMigrate(true)
			.baselineVersion(MigrationVersion.fromVersion("0"));
		if (target != null) {
			configuration.target(target);
		}
		return configuration.load();
	}

	private DriverManagerDataSource dataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource(
			postgres.getJdbcUrl(),
			postgres.getUsername(),
			postgres.getPassword()
		);
		dataSource.setDriverClassName(postgres.getDriverClassName());
		return dataSource;
	}

	private JdbcTemplate jdbcTemplate() {
		return new JdbcTemplate(dataSource());
	}

	private void assertMatchingIndexesCreated() {
		assertThat(jdbcTemplate().queryForObject("""
				select count(*)
				from pg_indexes
				where tablename = 'record_embeddings'
					and indexname in (
						'uk_record_embedding_record_model',
						'idx_record_embeddings_gemini_embedding_2_hvc'
					)
				""", Integer.class)).isEqualTo(2);
	}

	private void assertRunningInsightStatusAllowed() {
		assertThat(jdbcTemplate().queryForObject("""
				select pg_get_constraintdef(oid)
				from pg_constraint
				where conname = 'insights_status_check'
				""", String.class)).contains("RUNNING");
	}

	private void assertRecordAnswersNormalized() {
		assertThat(jdbcTemplate().queryForObject("""
				select count(*)
				from information_schema.columns
				where table_name = 'record_answers'
					and column_name in ('question_description', 'question_text', 'required', 'sort_order')
				""", Integer.class)).isZero();
		assertThat(jdbcTemplate().queryForObject("""
				select count(*)
				from information_schema.table_constraints
				where table_name = 'record_answers'
					and constraint_name = 'fk_record_answers_template_question'
					and constraint_type = 'FOREIGN KEY'
				""", Integer.class)).isEqualTo(1);
		assertThat(jdbcTemplate().queryForObject("""
				select count(*)
				from information_schema.columns
				where table_name = 'template_questions'
					and column_name = 'deleted_at'
				""", Integer.class)).isEqualTo(1);
	}

	private void seedLegacyRecordAnswers() {
		JdbcTemplate jdbcTemplate = jdbcTemplate();
		jdbcTemplate.update("""
			insert into users (id, created_at, updated_at, email, nickname, role)
			values ('00000000-0000-0000-0000-000000000001', now(), now(), 'legacy@test.com', '레거시', 'USER')
			""");
		jdbcTemplate.update("""
			insert into activity_type (id, created_at, updated_at, is_default, name, user_id)
			values (
				'00000000-0000-0000-0000-000000000002', now(), now(), false, '레거시 활동',
				'00000000-0000-0000-0000-000000000001'
			)
			""");
		jdbcTemplate.update("""
			insert into activity (
				id, created_at, updated_at, is_ongoing, started_at, status, title, activity_type_id, user_id
			)
			values (
				'00000000-0000-0000-0000-000000000003', now(), now(), true, current_date, 'IN_PROGRESS',
				'레거시 활동', '00000000-0000-0000-0000-000000000002',
				'00000000-0000-0000-0000-000000000001'
			)
			""");
		jdbcTemplate.update("""
			insert into templates (
				id, created_at, updated_at, builtin_code, builtin_version, is_builtin, title
			)
			values (?::uuid, now(), now(), 'LEGACY_TEMPLATE', 2, true, '레거시 템플릿')
			""", LEGACY_TEMPLATE_ID);
		jdbcTemplate.update("""
			insert into template_questions (
				id, created_at, updated_at, builtin_code, deleted_at, description,
				question_text, required, sort_order, template_id
			)
			values (?::uuid, now(), now(), 'LEGACY_CURRENT', null, '현재 설명', '현재 질문', false, 2, ?::uuid)
			""", CURRENT_QUESTION_ID, LEGACY_TEMPLATE_ID);
		jdbcTemplate.update("""
			insert into template_questions (
				id, created_at, updated_at, builtin_code, deleted_at, description,
				question_text, required, sort_order, template_id
			)
			values (?::uuid, now(), now(), 'LEGACY_DELETED', now(), '삭제 설명', '삭제 질문', true, 3, ?::uuid)
			""", DELETED_QUESTION_ID, LEGACY_TEMPLATE_ID);

		insertLegacyRecord(jdbcTemplate, "00000000-0000-0000-0000-000000000021", "orphan 기록");
		insertLegacyRecord(jdbcTemplate, "00000000-0000-0000-0000-000000000022", "수정된 질문 기록");
		insertLegacyRecord(jdbcTemplate, "00000000-0000-0000-0000-000000000023", "삭제된 질문 기록");
		insertLegacyAnswer(
			jdbcTemplate,
			ORPHAN_ANSWER_ID,
			"00000000-0000-0000-0000-000000000021",
			ORPHAN_QUESTION_ID,
			"사라진 질문",
			"사라진 설명",
			true,
			1
		);
		insertLegacyAnswer(
			jdbcTemplate,
			UPDATED_ANSWER_ID,
			"00000000-0000-0000-0000-000000000022",
			CURRENT_QUESTION_ID,
			"이전 질문",
			"이전 설명",
			true,
			1
		);
		insertLegacyAnswer(
			jdbcTemplate,
			DELETED_ANSWER_ID,
			"00000000-0000-0000-0000-000000000023",
			DELETED_QUESTION_ID,
			"삭제 질문",
			"삭제 설명",
			true,
			3
		);
	}

	private void insertLegacyRecord(JdbcTemplate jdbcTemplate, String id, String title) {
		jdbcTemplate.update("""
			insert into records (
				id, created_at, updated_at, status, title, version, activity_id, template_id, user_id
			)
			values (
				?::uuid, now(), now(), 'DRAFT', ?, 0,
				'00000000-0000-0000-0000-000000000003', ?::uuid,
				'00000000-0000-0000-0000-000000000001'
			)
			""", id, title, LEGACY_TEMPLATE_ID);
	}

	private void insertLegacyAnswer(
		JdbcTemplate jdbcTemplate,
		String id,
		String recordId,
		String questionId,
		String questionText,
		String questionDescription,
		boolean required,
		int sortOrder
	) {
		jdbcTemplate.update("""
			insert into record_answers (
				id, created_at, updated_at, answer_text, question_description, question_text,
				required, sort_order, template_question_id, record_id
			)
			values (?::uuid, now(), now(), '레거시 답변', ?, ?, ?, ?, ?::uuid, ?::uuid)
			""", id, questionDescription, questionText, required, sortOrder, questionId, recordId);
	}

	private void assertHistoryAnswer(
		String answerId,
		String originalQuestionId,
		String questionText,
		String description,
		boolean required,
		int sortOrder,
		boolean remapped
	) {
		Map<String, Object> row = jdbcTemplate().queryForMap("""
			select answer.template_question_id::text as question_id,
				question.template_id::text as template_id,
				question.question_text,
				question.description,
				question.required,
				question.sort_order,
				question.deleted_at
			from record_answers answer
			join template_questions question on question.id = answer.template_question_id
			where answer.id = ?::uuid
			""", answerId);

		if (remapped) {
			assertThat(row.get("question_id")).isNotEqualTo(originalQuestionId);
		} else {
			assertThat(row.get("question_id")).isEqualTo(originalQuestionId);
		}
		assertThat(row)
			.containsEntry("template_id", LEGACY_TEMPLATE_ID)
			.containsEntry("question_text", questionText)
			.containsEntry("description", description)
			.containsEntry("required", required)
			.containsEntry("sort_order", sortOrder);
		assertThat(row.get("deleted_at")).isNotNull();
	}

	private void assertLegacyTemplateQuestionsRemainSeparated() {
		JdbcTemplate jdbcTemplate = jdbcTemplate();
		assertThat(jdbcTemplate.queryForObject("""
			select count(*)
			from template_questions
			where template_id = ?::uuid
			""", Integer.class, LEGACY_TEMPLATE_ID)).isEqualTo(4);
		assertThat(jdbcTemplate.queryForObject("""
			select count(*)
			from template_questions
			where template_id = ?::uuid
				and deleted_at is null
			""", Integer.class, LEGACY_TEMPLATE_ID)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForMap("""
			select question_text, description, required, sort_order, deleted_at
			from template_questions
			where id = ?::uuid
			""", CURRENT_QUESTION_ID))
			.containsEntry("question_text", "현재 질문")
			.containsEntry("description", "현재 설명")
			.containsEntry("required", false)
			.containsEntry("sort_order", 2)
			.containsEntry("deleted_at", null);
	}
}
