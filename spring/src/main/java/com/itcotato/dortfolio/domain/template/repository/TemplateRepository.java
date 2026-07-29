package com.itcotato.dortfolio.domain.template.repository;

import com.itcotato.dortfolio.domain.template.entity.Template;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TemplateRepository extends JpaRepository<Template, UUID> {

	@EntityGraph(attributePaths = "questions")
	Optional<Template> findByBuiltinCode(String builtinCode);

	@EntityGraph(attributePaths = "questions")
	@Query("""
		select t
		from Template t
		where t.deletedAt is null
			and (t.isBuiltin = true or t.user.id = :userId)
		order by t.isBuiltin desc,
			case t.builtinCode
				when 'IDEA_PLANNING' then 1
				when 'COLLABORATION_CONFLICT' then 2
				when 'PROBLEM_SOLVING_RESULT' then 3
				when 'IMMERSION_CHALLENGE' then 4
				else 5
			end,
			t.createdAt desc
		""")
	List<Template> findAvailableTemplates(@Param("userId") UUID userId);

	@EntityGraph(attributePaths = "questions")
	Optional<Template> findByIdAndDeletedAtIsNull(UUID id);

	@EntityGraph(attributePaths = "questions")
	List<Template> findAllByIdInAndDeletedAtIsNull(List<UUID> ids);

	@EntityGraph(attributePaths = "questions")
	List<Template> findAllByBuiltinCodeInAndDeletedAtIsNull(List<String> builtinCodes);

    @EntityGraph(attributePaths = "questions")
    Optional<Template> findWithQuestionsById(UUID id);
}
