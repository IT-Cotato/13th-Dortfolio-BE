package com.itcotato.dortfolio.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.output.MigrateResult;
import org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationTest {

	@Container
	private final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
		DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres")
	);

	@Test
	void migratesFreshSchemaThroughLatestVersion() {
		MigrateResult result = flyway().migrate();

		assertThat(result.migrationsExecuted).isEqualTo(20);
		assertThat(result.targetSchemaVersion).isEqualTo("20");
		assertMatchingIndexesCreated();
		assertRunningInsightStatusAllowed();
		assertUserOwnedDataCascadesOnDelete();
		assertSinglePrimaryUserJobConstraintCreated();
		assertRecordAnswersNormalized();
		assertTemplateDescriptionsExtended();
		assertBuiltinTemplatesSeeded();
		assertJobCatalogSeeded();
		assertRecordStrengthTablesCreated();
		assertStrengthTagCatalogSeeded();
		assertRecommendationNoMatchSupported();
		assertRecordAnalysisJobOutboxCreated();
	}

	private void assertRecordAnalysisJobOutboxCreated() {
		assertThat(jdbcTemplate().queryForObject("""
				select count(*)
				from information_schema.tables
				where table_name = 'record_analysis_jobs'
				""", Integer.class)).isEqualTo(1);
		assertThat(jdbcTemplate().queryForObject("""
				select count(*)
				from information_schema.columns
				where table_name = 'record_analysis'
				  and column_name = 'analysis_generation'
				""", Integer.class)).isEqualTo(1);
	}

	private void assertRecordStrengthTablesCreated() {
		assertThat(jdbcTemplate().queryForObject("""
				select count(*)
				from information_schema.tables
				where table_name in ('strength_tags', 'strength_tag_embeddings', 'record_strength_tags')
				""", Integer.class)).isEqualTo(3);
		assertThat(jdbcTemplate().queryForObject("""
				select count(*)
				from information_schema.columns
				where table_name = 'record_strength_tags'
				  and column_name = 'cosine_similarity'
				""", Integer.class)).isEqualTo(1);
	}

	private void assertStrengthTagCatalogSeeded() {
		assertThat(jdbcTemplate().queryForObject(
			"select count(*) from strength_tags",
			Integer.class
		)).isEqualTo(30);
		assertThat(jdbcTemplate().queryForObject("""
				select count(*)
				from strength_tags
				where description is not null
				  and evaluation_criteria is not null
				  and positive_example is not null
				  and negative_example is not null
				""", Integer.class)).isEqualTo(30);
	}

	private void assertRecommendationNoMatchSupported() {
		assertThat(jdbcTemplate().queryForObject("""
				select is_nullable
				from information_schema.columns
				where table_name = 'insight_job_recommendations'
				  and column_name = 'match_status'
				""", String.class)).isEqualTo("NO");
		assertThat(jdbcTemplate().queryForObject("""
				select is_nullable
				from information_schema.columns
				where table_name = 'insight_job_recommendations'
				  and column_name = 'record_id_snapshot'
				""", String.class)).isEqualTo("YES");
		assertThat(jdbcTemplate().queryForObject("""
				select count(*)
				from pg_constraint
				where conname in (
				    'ck_insight_job_recommendation_match_status',
				    'ck_insight_job_recommendation_match_result'
				)
				""", Integer.class)).isEqualTo(2);
	}

	@Test
	void correctsExistingBuiltinTemplateWithoutChangingIds() {
		flyway(MigrationVersion.fromVersion("14")).migrate();
		UUID templateId = jdbcTemplate().queryForObject(
			"select id from templates where builtin_code = 'IDEA_PLANNING'", UUID.class
		);
		UUID questionId = jdbcTemplate().queryForObject(
			"select id from template_questions where builtin_code = 'IDEA_PLANNING_BACKGROUND'", UUID.class
		);
		jdbcTemplate().update("""
				update templates
				set description = '잘못된 설명'
				where builtin_code = 'IDEA_PLANNING'
				""");
		jdbcTemplate().update("""
				update template_questions
				set description = '잘못된 항목 설명', question_text = '잘못된 항목', required = false
				where builtin_code = 'IDEA_PLANNING_BACKGROUND'
				""");

		flyway().migrate();

		assertThat(jdbcTemplate().queryForObject(
			"select id from templates where builtin_code = 'IDEA_PLANNING'", UUID.class
		)).isEqualTo(templateId);
		assertThat(jdbcTemplate().queryForObject(
			"select id from template_questions where builtin_code = 'IDEA_PLANNING_BACKGROUND'", UUID.class
		)).isEqualTo(questionId);
		assertBuiltinTemplatesSeeded();
	}

	private Flyway flyway() {
		return flyway(null);
	}

	private Flyway flyway(MigrationVersion target) {
		FluentConfiguration configuration = Flyway.configure()
			.dataSource(dataSource())
			.locations("classpath:db/migration")
			.baselineOnMigrate(true)
			.baselineVersion(MigrationVersion.fromVersion("0"));
		if (target != null) {
			configuration.target(target);
		}
		configuration.getConfigurationExtension(PostgreSQLConfigurationExtension.class)
			.setTransactionalLock(false);
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
				""", Integer.class)).isZero();
	}

	private void assertUserOwnedDataCascadesOnDelete() {
		assertThat(jdbcTemplate().queryForObject("""
				select count(*)
				from pg_constraint
				where confrelid = 'users'::regclass
					and confdeltype = 'c'
				""", Integer.class)).isEqualTo(10);
	}

	private void assertSinglePrimaryUserJobConstraintCreated() {
		assertThat(jdbcTemplate().queryForObject("""
				select count(*)
				from pg_index i
				join pg_class index_class on index_class.oid = i.indexrelid
				join pg_class table_class on table_class.oid = i.indrelid
				join pg_attribute attribute
					on attribute.attrelid = table_class.oid
					and attribute.attnum = i.indkey[0]
				where table_class.relname = 'user_jobs'
					and index_class.relname = 'uq_user_jobs_single_primary'
					and i.indisunique = true
					and attribute.attname = 'user_id'
					and pg_get_expr(i.indpred, i.indrelid) = '(is_primary = true)'
				""", Integer.class)).isEqualTo(1);
	}

	private void assertTemplateDescriptionsExtended() {
		assertThat(jdbcTemplate().queryForObject("""
				select character_maximum_length
				from information_schema.columns
				where table_name = 'templates'
					and column_name = 'description'
				""", Integer.class)).isEqualTo(255);
		assertThat(jdbcTemplate().queryForObject("""
				select character_maximum_length
				from information_schema.columns
				where table_name = 'template_questions'
					and column_name = 'description'
				""", Integer.class)).isEqualTo(255);
	}

	private void assertBuiltinTemplatesSeeded() {
		assertTemplate(
			"IDEA_PLANNING",
			"아이디어·기획",
			"아이디어를 내고 더 나은 방향을 선택했던 경험을 기록해보세요.",
			List.of(
				new SeedQuestion("문제 정의", "오늘 회의나 일과 중에 해결해야 했던 과제나 새롭게 발견한 불편함은 무엇인가요?", 1),
				new SeedQuestion("아이디어 나열", "이 문제를 해결하기 위해 회의나 머릿속에서 제안된 아이디어에는 어떤 것들이 있었나요?", 2),
				new SeedQuestion("나만의 선택 기준", "수많은 대안 중 오늘 최종 방향을 결정짓게 만든 '가장 중요한 판단 기준'은 무엇이었나요?", 3),
				new SeedQuestion("기획 인사이트", "오늘 아이디어를 내고 판단하는 과정에서 새롭게 깨달은 '나만의 기준'이나 '효과적이었던 방식'은 무엇인가요?", 4)
			)
		);
		assertTemplate(
			"COLLABORATION_CONFLICT",
			"협업·갈등",
			"함께 일하며 의견을 맞춰갔던 경험을 기록해보세요.",
			List.of(
				new SeedQuestion("갈등 상황", "협업 과정에서 팀원 간(혹은 나와 팀원 간)에 부딪힌 의견 차이나 협업의 걸림돌은 무엇이었나요?", 1),
				new SeedQuestion("입장 분석", "대립하는 각 주장의 핵심 논리는 무엇이었으며, 각각 어떤 장단점을 가지고 있었나요?", 2),
				new SeedQuestion("나만의 조율 기준", "이 갈등을 해결하거나 중재하기 위해 내가 가장 중요하게 생각한 '판단 기준'은 무엇이었나요?", 3),
				new SeedQuestion("행동과 최종 합의", "내가 세운 기준을 바탕으로 팀원들과 어떻게 소통했으며, 최종적으로 도출한 합의점은 무엇인가요?", 4),
				new SeedQuestion("협업 인사이트", "갈등을 조율하는 과정에서 새롭게 깨달은 '나만의 협업 규칙'이나 '효과적이었던 소통 방식'은 무엇인가요?", 5)
			)
		);
		assertTemplate(
			"PROBLEM_SOLVING_RESULT",
			"문제해결·성과",
			"예상치 못한 문제를 논리적으로 해결한 경험을 기록해보세요.",
			List.of(
				new SeedQuestion("문제 상황", "계획과 달리 갑자기 터진 오류나 예상치 못한 난관은 무엇이었나요?", 1),
				new SeedQuestion("원인 가설", "이 문제가 발생한 '가장 유력한 원인'은 무엇이라고 추정했나요?", 2),
				new SeedQuestion("의사결정", "문제를 해결하기 위해 어떤 대안들을 고려했고, 왜 그 방향(순서)대로 실행했나요?", 3),
				new SeedQuestion("문제 해결 결과", "내가 조치한 결과 상황이 어떻게 정상화되었으며, 어떤 정량적/정성적 성과로 이어졌나요?", 4),
				new SeedQuestion("문제 해결 인사이트", "다음번에 이와 비슷한 문제가 또 터지지 않게 하려면 어떤 예방책이나 규칙이 필요할까요?", 5)
			)
		);
		assertTemplate(
			"IMMERSION_CHALLENGE",
			"몰입·도전",
			"스스로 더 높은 목표를 세우고 몰입했던 경험을 기록해보세요.",
			List.of(
				new SeedQuestion("나의 목표", "기존 방식에 안주하지 않고, 오늘 일부러 '더 높은 기준'을 적용해 시도한 일은 무엇인가요?", 1),
				new SeedQuestion("방해 요소", "목표에 도전하면서 오늘 나를 가장 지치게 하거나 유혹했던 '방해 요소'는 무엇이었나요?", 2),
				new SeedQuestion("나만의 행동 원칙", "포기하거나 타협하지 않고 끝까지 몰입하기 위해 스스로 부여한 '나만의 행동 원칙'은 무엇이었나요?", 3),
				new SeedQuestion("몰입의 결과", "집요하게 몰입한 결과, 어떤 결과물을 만들어냈거나 개인적인 성장을 이뤘나요?", 4),
				new SeedQuestion("인사이트", "나는 어떤 환경이나 마인드셋일 때 가장 폭발적으로 몰입하고 성장하나요?", 5)
			)
		);
	}

	private void assertTemplate(String code, String title, String description, List<SeedQuestion> expectedQuestions) {
		assertThat(jdbcTemplate().queryForObject(
			"select title from templates where builtin_code = ?", String.class, code
		)).isEqualTo(title);
		assertThat(jdbcTemplate().queryForObject(
			"select description from templates where builtin_code = ?", String.class, code
		)).isEqualTo(description);
		assertThat(jdbcTemplate().queryForObject(
			"select builtin_version from templates where builtin_code = ?", Integer.class, code
		)).isEqualTo(2);

		List<SeedQuestion> questions = jdbcTemplate().query("""
				select q.question_text, q.description, q.sort_order, q.required
				from template_questions q
				join templates t on t.id = q.template_id
				where t.builtin_code = ?
				order by q.sort_order
				""", (resultSet, rowNumber) -> {
			assertThat(resultSet.getBoolean("required")).isTrue();
			return new SeedQuestion(
				resultSet.getString("question_text"),
				resultSet.getString("description"),
				resultSet.getInt("sort_order")
			);
		}, code);
		assertThat(questions).containsExactlyElementsOf(expectedQuestions);
	}

    private void assertJobCatalogSeeded() {
        assertThat(jdbcTemplate().queryForObject(
                "select count(*) from jobs",
                Integer.class
        )).isEqualTo(53);

        assertThat(jdbcTemplate().queryForObject(
                "select count(*) from competency_tags",
                Integer.class
        )).isEqualTo(237);

        assertThat(jdbcTemplate().queryForObject(
                "select count(*) from job_competencies",
                Integer.class
        )).isEqualTo(265);

        assertThat(jdbcTemplate().queryForObject("""
        select count(*)
        from (
            select job_id
            from job_competencies
            group by job_id
            having count(*) <> 5
        ) invalid_jobs
        """, Integer.class
        )).isZero();
    }

	private record SeedQuestion(String questionText, String description, int sortOrder) {
	}
}
