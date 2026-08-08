package com.itcotato.dortfolio.domain.insight.entity;

import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.UUID;

@Getter
@Entity
@Table(
        name = "insight_job_recommendations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_insight_job_competency",
                columnNames = {"insight_id", "job_competency_id_snapshot"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InsightJobRecommendation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insight_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Insight insight;

    @Column(name = "job_competency_id_snapshot", nullable = false)
    private UUID jobCompetencyIdSnapshot;

    @Column(name = "competency_name_snapshot", nullable = false)
    private String competencyNameSnapshot;

    @Column(name = "sort_order_snapshot", nullable = false)
    private int sortOrderSnapshot;

    @Column(name = "record_id_snapshot", nullable = false)
    private UUID recordIdSnapshot;

    @Column(name = "record_title_snapshot", nullable = false)
    private String recordTitleSnapshot;

    @Column(name = "template_name_snapshot", nullable = false)
    private String templateNameSnapshot;

    @Column(columnDefinition = "text", nullable = false)
    private String reason;

    @Column(nullable = false)
    private double similarity;

    private InsightJobRecommendation(
            Insight insight,
            UUID jobCompetencyIdSnapshot,
            String competencyNameSnapshot,
            int sortOrderSnapshot,
            UUID recordIdSnapshot,
            String recordTitleSnapshot,
            String templateNameSnapshot,
            String reason,
            double similarity
    ) {
        this.insight = insight;
        this.jobCompetencyIdSnapshot = jobCompetencyIdSnapshot;
        this.competencyNameSnapshot = competencyNameSnapshot;
        this.sortOrderSnapshot = sortOrderSnapshot;
        this.recordIdSnapshot = recordIdSnapshot;
        this.recordTitleSnapshot = recordTitleSnapshot;
        this.templateNameSnapshot = templateNameSnapshot;
        this.reason = reason;
        this.similarity = similarity;
    }

    public static InsightJobRecommendation create(
            Insight insight,
            UUID jobCompetencyIdSnapshot,
            String competencyNameSnapshot,
            int sortOrderSnapshot,
            UUID recordIdSnapshot,
            String recordTitleSnapshot,
            String templateNameSnapshot,
            String reason,
            double similarity
    ) {
        return new InsightJobRecommendation(
                insight,
                jobCompetencyIdSnapshot,
                competencyNameSnapshot,
                sortOrderSnapshot,
                recordIdSnapshot,
                recordTitleSnapshot,
                templateNameSnapshot,
                reason,
                similarity
        );
    }
}
