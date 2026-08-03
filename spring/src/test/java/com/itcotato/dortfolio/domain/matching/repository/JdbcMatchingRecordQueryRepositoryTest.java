package com.itcotato.dortfolio.domain.matching.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.itcotato.dortfolio.domain.matching.config.MatchingProperties;
import com.itcotato.dortfolio.domain.matching.model.MatchingRecordCandidate;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class JdbcMatchingRecordQueryRepositoryTest {

	private static final String EMBEDDING_MODEL = "gemini-embedding-2";
	private static final int EMBEDDING_DIMENSION = 3072;

	@Container
	private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
		DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres")
	);

	private JdbcTemplate jdbcTemplate;
	private JdbcMatchingRecordQueryRepository repository;
	private MatchingProperties matchingProperties;

	@BeforeEach
	void setUp() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource(
			POSTGRES.getJdbcUrl(),
			POSTGRES.getUsername(),
			POSTGRES.getPassword()
		);
		dataSource.setDriverClassName(POSTGRES.getDriverClassName());
		jdbcTemplate = new JdbcTemplate(dataSource);
		matchingProperties = new MatchingProperties(
			3,
			10,
			10,
			EMBEDDING_DIMENSION,
			List.of(EMBEDDING_MODEL),
			500,
			30,
			20,
			Duration.ofDays(1),
			List.of(new MatchingProperties.QuestionTag("TEST", "테스트 문항"))
		);
		repository = new JdbcMatchingRecordQueryRepository(jdbcTemplate, matchingProperties, new DataSourceTransactionManager(dataSource));

		createSchema();
		createVectorIndexForTest();
	}

	@Test
	void vectorCandidateQueryUsesHalfvecHnswIndexPlan() {
		UUID userId = UUID.randomUUID();
		UUID activityTypeId = insertActivityType("동아리");
		UUID templateId = insertTemplate("협업 갈등");
		UUID activityId = insertActivity(userId, activityTypeId, "창업 동아리 활동");
		LocalDateTime currentUpdatedAt = LocalDateTime.of(2026, 8, 4, 10, 0);
		for (int index = 0; index < 30; index++) {
			UUID recordId = insertRecord(userId, activityId, templateId, "계획 검증 기록 " + index, currentUpdatedAt, null);
			insertEmbedding(recordId, vector(1.0f - (index * 0.01f), index * 0.01f));
			insertAnalysis(recordId, "COMPLETED", currentUpdatedAt, "계획 검증 요약");
			insertAnswer(recordId, "상황", "답변입니다.");
		}

		Integer indexCount = jdbcTemplate.queryForObject("""
				select count(*)
				from pg_indexes
				where tablename = 'record_embeddings'
					and indexdef like '%halfvec(3072)%'
					and indexdef like '%halfvec_cosine_ops%'
					and indexdef like '%gemini-embedding-2%'
				""", Integer.class);
		assertThat(indexCount).isEqualTo(1);

		List<String> plan = explainVectorCandidateQuery(userId);

		assertThat(String.join("\n", plan)).contains("Index Scan using idx_record_embeddings_gemini_embedding_2_hvc");
	}

	private List<String> explainVectorCandidateQuery(UUID userId) {
		String sql = "explain (costs off) " + repository.buildVectorCandidateSql(EMBEDDING_MODEL);
		String queryVector = toVectorLiteral(vector(1.0f, 0.0f));
		return jdbcTemplate.execute((org.springframework.jdbc.core.ConnectionCallback<List<String>>) connection -> {
			boolean previousAutoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);
			try (
				PreparedStatement iterativeScanSetting = connection.prepareStatement("set local hnsw.iterative_scan = strict_order");
				PreparedStatement seqScanSetting = connection.prepareStatement("set local enable_seqscan = off");
				PreparedStatement statement = connection.prepareStatement(sql)
			) {
				iterativeScanSetting.execute();
				seqScanSetting.execute();
				statement.setString(1, queryVector);
				statement.setObject(2, userId);
				statement.setString(3, queryVector);
				statement.setInt(4, 10);
				List<String> plan = readPlan(statement);
				connection.rollback();
				return plan;
			} finally {
				connection.setAutoCommit(previousAutoCommit);
			}
		});
	}

	private List<String> readPlan(PreparedStatement statement) throws SQLException {
		List<String> plan = new ArrayList<>();
		try (ResultSet resultSet = statement.executeQuery()) {
			while (resultSet.next()) {
				plan.add(resultSet.getString(1));
			}
		}
		return plan;
	}

	@Test
	void findVectorCandidatesFiltersAndSortsWithPgvector() {
		UUID userId = UUID.randomUUID();
		UUID otherUserId = UUID.randomUUID();
		UUID activityTypeId = insertActivityType("동아리");
		UUID templateId = insertTemplate("협업 갈등");
		UUID activityId = insertActivity(userId, activityTypeId, "창업 동아리 활동");

		LocalDateTime currentUpdatedAt = LocalDateTime.of(2026, 8, 4, 10, 0);
		UUID closestRecordId = insertRecord(userId, activityId, templateId, "가장 가까운 기록", currentUpdatedAt, null);
		UUID secondRecordId = insertRecord(userId, activityId, templateId, "두 번째 기록", currentUpdatedAt, null);
		UUID staleRecordId = insertRecord(userId, activityId, templateId, "오래된 분석 기록", currentUpdatedAt, null);
		UUID emptyAnswerRecordId = insertRecord(userId, activityId, templateId, "빈 답변 기록", currentUpdatedAt, null);
		UUID failedAnalysisRecordId = insertRecord(userId, activityId, templateId, "분석 실패 기록", currentUpdatedAt, null);
		UUID deletedRecordId = insertRecord(userId, activityId, templateId, "삭제된 기록", currentUpdatedAt, currentUpdatedAt);
		UUID otherUserRecordId = insertRecord(otherUserId, activityId, templateId, "다른 사용자 기록", currentUpdatedAt, null);

		insertEmbedding(closestRecordId, vector(1.0f, 0.0f));
		insertEmbedding(secondRecordId, vector(0.8f, 0.2f));
		insertEmbedding(staleRecordId, vector(1.0f, 0.0f));
		insertEmbedding(emptyAnswerRecordId, vector(1.0f, 0.0f));
		insertEmbedding(failedAnalysisRecordId, vector(1.0f, 0.0f));
		insertEmbedding(deletedRecordId, vector(1.0f, 0.0f));
		insertEmbedding(otherUserRecordId, vector(1.0f, 0.0f));

		insertAnalysis(closestRecordId, "COMPLETED", currentUpdatedAt, "가까운 기록 요약");
		insertAnalysis(secondRecordId, "COMPLETED", currentUpdatedAt, "두 번째 기록 요약");
		insertAnalysis(staleRecordId, "COMPLETED", currentUpdatedAt.minusDays(1), "오래된 요약");
		insertAnalysis(emptyAnswerRecordId, "COMPLETED", currentUpdatedAt, "빈 답변 요약");
		insertAnalysis(failedAnalysisRecordId, "FAILED", currentUpdatedAt, "실패 요약");
		insertAnalysis(deletedRecordId, "COMPLETED", currentUpdatedAt, "삭제 요약");
		insertAnalysis(otherUserRecordId, "COMPLETED", currentUpdatedAt, "다른 사용자 요약");

		insertAnswer(closestRecordId, "상황", "팀 미팅에서 갈등을 조율했습니다.");
		insertAnswer(secondRecordId, "상황", "협업 과정에서 의견을 정리했습니다.");
		insertAnswer(staleRecordId, "상황", "예전 답변입니다.");
		insertAnswer(emptyAnswerRecordId, "상황", "   ");
		insertAnswer(failedAnalysisRecordId, "상황", "분석 실패 답변입니다.");
		insertAnswer(deletedRecordId, "상황", "삭제된 답변입니다.");
		insertAnswer(otherUserRecordId, "상황", "다른 사용자 답변입니다.");

		List<MatchingRecordCandidate> candidates = repository.findVectorCandidates(
			userId,
			EMBEDDING_MODEL,
			vector(1.0f, 0.0f),
			10
		);

		assertThat(candidates)
			.extracting(MatchingRecordCandidate::recordId)
			.containsExactly(closestRecordId, secondRecordId);
		assertThat(candidates.get(0).activityTitle()).isEqualTo("창업 동아리 활동");
		assertThat(candidates.get(0).templateTitle()).isEqualTo("협업 갈등");
		assertThat(candidates.get(0).summary()).isEqualTo("가까운 기록 요약");
		assertThat(candidates.get(0).matchRate()).isGreaterThanOrEqualTo(candidates.get(1).matchRate());
	}

	@Test
	void findVectorCandidatesReturnsLimitRecordsWhenOtherUsersHaveCloserEmbeddings() {
		UUID userId = UUID.randomUUID();
		UUID otherUserId = UUID.randomUUID();
		UUID activityTypeId = insertActivityType("동아리");
		UUID templateId = insertTemplate("협업 갈등");
		UUID activityId = insertActivity(userId, activityTypeId, "창업 동아리 활동");
		UUID otherActivityId = insertActivity(otherUserId, activityTypeId, "다른 사용자 활동");
		LocalDateTime currentUpdatedAt = LocalDateTime.of(2026, 8, 4, 10, 0);

		for (int index = 0; index < 120; index++) {
			UUID recordId = insertRecord(otherUserId, otherActivityId, templateId, "다른 사용자 기록 " + index, currentUpdatedAt, null);
			insertEmbedding(recordId, vector(1.0f - (index * 0.0001f), index * 0.0001f));
			insertAnalysis(recordId, "COMPLETED", currentUpdatedAt, "다른 사용자 요약");
			insertAnswer(recordId, "상황", "다른 사용자 답변입니다.");
		}

		UUID firstRecordId = insertValidMatchingRecord(userId, activityId, templateId, "대상 사용자 기록 1", currentUpdatedAt, vector(0.6f, 0.4f));
		UUID secondRecordId = insertValidMatchingRecord(userId, activityId, templateId, "대상 사용자 기록 2", currentUpdatedAt, vector(0.5f, 0.5f));
		UUID thirdRecordId = insertValidMatchingRecord(userId, activityId, templateId, "대상 사용자 기록 3", currentUpdatedAt, vector(0.4f, 0.6f));

		List<MatchingRecordCandidate> candidates = repository.findVectorCandidates(
			userId,
			EMBEDDING_MODEL,
			vector(1.0f, 0.0f),
			3
		);

		assertThat(candidates)
			.extracting(MatchingRecordCandidate::recordId)
			.containsExactly(firstRecordId, secondRecordId, thirdRecordId);
	}

	private UUID insertValidMatchingRecord(
		UUID userId,
		UUID activityId,
		UUID templateId,
		String title,
		LocalDateTime currentUpdatedAt,
		float[] embedding
	) {
		UUID recordId = insertRecord(userId, activityId, templateId, title, currentUpdatedAt, null);
		insertEmbedding(recordId, embedding);
		insertAnalysis(recordId, "COMPLETED", currentUpdatedAt, title + " 요약");
		insertAnswer(recordId, "상황", title + " 답변입니다.");
		return recordId;
	}

	private void createSchema() {
		jdbcTemplate.execute("create extension if not exists vector");
		jdbcTemplate.execute("drop table if exists record_answers");
		jdbcTemplate.execute("drop table if exists record_analysis");
		jdbcTemplate.execute("drop table if exists record_embeddings");
		jdbcTemplate.execute("drop table if exists records");
		jdbcTemplate.execute("drop table if exists templates");
		jdbcTemplate.execute("drop table if exists activity");
		jdbcTemplate.execute("drop table if exists activity_type");
		jdbcTemplate.execute("""
				create table activity_type (
					id uuid primary key,
					name text not null
				)
			""");
		jdbcTemplate.execute("""
				create table activity (
					id uuid primary key,
					user_id uuid not null,
					activity_type_id uuid not null references activity_type(id),
					title text not null,
					started_at date,
					ended_at date,
					is_ongoing boolean not null,
					deleted_at timestamp
				)
			""");
		jdbcTemplate.execute("""
				create table templates (
					id uuid primary key,
					title text not null,
					deleted_at timestamp
				)
			""");
		jdbcTemplate.execute("""
				create table records (
					id uuid primary key,
					user_id uuid not null,
					activity_id uuid not null references activity(id),
					template_id uuid not null references templates(id),
					title text not null,
					status text not null,
					completed_at timestamp,
					updated_at timestamp not null,
					deleted_at timestamp
				)
			""");
		jdbcTemplate.execute("""
				create table record_embeddings (
					id uuid primary key,
					record_id uuid not null references records(id),
					embedding_model text not null,
					embedding vector not null,
					constraint uk_record_embedding_record_model unique (record_id, embedding_model)
				)
			""");
		jdbcTemplate.execute("""
				create table record_analysis (
					id uuid primary key,
					record_id uuid not null unique references records(id),
					ai_analysis_status text not null,
					analyzed_record_updated_at timestamp,
					summary text
				)
			""");
		jdbcTemplate.execute("""
				create table record_answers (
					id uuid primary key,
					record_id uuid not null references records(id),
					question_text text not null,
					answer_text text not null,
					sort_order integer not null
				)
				""");
	}

	private void createVectorIndexForTest() {
		jdbcTemplate.execute("""
				create index idx_record_embeddings_gemini_embedding_2_hvc
				on record_embeddings
				using hnsw ((embedding::halfvec(3072)) halfvec_cosine_ops)
				where embedding_model = 'gemini-embedding-2'
			""");
	}

	private UUID insertActivityType(String name) {
		UUID id = UUID.randomUUID();
		jdbcTemplate.update("insert into activity_type (id, name) values (?, ?)", id, name);
		return id;
	}

	private UUID insertTemplate(String title) {
		UUID id = UUID.randomUUID();
		jdbcTemplate.update("insert into templates (id, title) values (?, ?)", id, title);
		return id;
	}

	private UUID insertActivity(UUID userId, UUID activityTypeId, String title) {
		UUID id = UUID.randomUUID();
		jdbcTemplate.update("""
				insert into activity (id, user_id, activity_type_id, title, started_at, ended_at, is_ongoing)
				values (?, ?, ?, ?, ?, ?, ?)
			""", id, userId, activityTypeId, title, LocalDate.of(2026, 3, 1), null, true);
		return id;
	}

	private UUID insertRecord(
		UUID userId,
		UUID activityId,
		UUID templateId,
		String title,
		LocalDateTime updatedAt,
		LocalDateTime deletedAt
	) {
		UUID id = UUID.randomUUID();
		jdbcTemplate.update("""
				insert into records (id, user_id, activity_id, template_id, title, status, completed_at, updated_at, deleted_at)
				values (?, ?, ?, ?, ?, 'COMPLETED', ?, ?, ?)
			""", id, userId, activityId, templateId, title, updatedAt, updatedAt, deletedAt);
		return id;
	}

	private void insertEmbedding(UUID recordId, float[] embedding) {
		jdbcTemplate.update("""
				insert into record_embeddings (id, record_id, embedding_model, embedding)
				values (?, ?, ?, cast(? as vector))
			""", UUID.randomUUID(), recordId, EMBEDDING_MODEL, toVectorLiteral(embedding));
	}

	private void insertAnalysis(UUID recordId, String status, LocalDateTime analyzedRecordUpdatedAt, String summary) {
		jdbcTemplate.update("""
				insert into record_analysis (id, record_id, ai_analysis_status, analyzed_record_updated_at, summary)
				values (?, ?, ?, ?, ?)
			""", UUID.randomUUID(), recordId, status, analyzedRecordUpdatedAt, summary);
	}

	private void insertAnswer(UUID recordId, String questionText, String answerText) {
		jdbcTemplate.update("""
				insert into record_answers (id, record_id, question_text, answer_text, sort_order)
				values (?, ?, ?, ?, ?)
			""", UUID.randomUUID(), recordId, questionText, answerText, 1);
	}

	private float[] vector(float first, float second) {
		float[] embedding = new float[EMBEDDING_DIMENSION];
		embedding[0] = first;
		embedding[1] = second;
		return embedding;
	}

	private String toVectorLiteral(float[] embedding) {
		StringBuilder builder = new StringBuilder("[");
		for (int index = 0; index < embedding.length; index++) {
			if (index > 0) {
				builder.append(',');
			}
			builder.append(Float.toString(embedding[index]));
		}
		return builder.append(']').toString();
	}
}
