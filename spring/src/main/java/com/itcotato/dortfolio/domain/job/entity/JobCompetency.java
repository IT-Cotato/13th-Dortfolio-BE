package com.itcotato.dortfolio.domain.job.entity;

import com.itcotato.dortfolio.domain.record.entity.CompetencyTag;
import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "job_competencies",
	uniqueConstraints = {
		@UniqueConstraint(
                name = "uk_job_competency",
                columnNames = {"job_id", "competency_tag_id"}
        ),
            @UniqueConstraint(
                    name = "uk_job_competency_sort_order",
                    columnNames = {"job_id", "sort_order"}
            )
	    }
    )

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobCompetency extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "job_id", nullable = false)
	private Job job;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "competency_tag_id", nullable = false)
	private CompetencyTag competencyTag;

	@Column(nullable = false)
	private int sortOrder;

	private JobCompetency(Job job, CompetencyTag competencyTag, int sortOrder) {
		this.job = job;
		this.competencyTag = competencyTag;
		this.sortOrder = sortOrder;
	}

	public static JobCompetency create(Job job, CompetencyTag competencyTag, int sortOrder) {
		return new JobCompetency(job, competencyTag, sortOrder);
	}
}
