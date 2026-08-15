package com.itcotato.dortfolio.domain.job.entity;

import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "jobs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Job extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 50)
    private String categoryCode;

	@Column(nullable = false)
	private String name;

	@Column
	private String description;

	private Job(
            String code,
            String categoryCode,
            String name,
            String description
    ) {
        this.code = code;
        this.categoryCode = this.categoryCode;
		this.name = name;
		this.description = description;
	}

	public static Job create(
            String code,
            String categoryCode,
            String name,
            String description
    ) {
		return new Job(code, categoryCode, name, description);
	}
}
