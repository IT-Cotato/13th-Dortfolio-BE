package com.itcotato.dortfolio.domain.record.entity;

import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "competency_tags")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompetencyTag extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String code;

	@Column(nullable = false)
	private String name;

	@Column
	private String description;

	private CompetencyTag(
            String code,
            String name,
            String description
    ) {
        this.code = code;
		this.name = name;
		this.description = description;
	}

	public static CompetencyTag create(
            String code,
            String name,
            String description
    ) {
		return new CompetencyTag(code, name, description);
	}
}
