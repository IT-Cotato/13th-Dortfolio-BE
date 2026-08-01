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
        name = "insight_template_statistics",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_insight_template_statistic",
                columnNames = {"insight_id", "template_id_snapshot"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InsightTemplateStatistic extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insight_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Insight insight;

    @Column(name = "template_id_snapshot", nullable = false)
    private UUID templateIdSnapshot;

    @Column(name = "template_name_snapshot", nullable = false)
    private String templateNameSnapshot;

    @Column(nullable = false)
    private int recordCount;

    @Column(nullable = false)
    private double ratio;

    @Column(nullable = false)
    private int rank;

    private InsightTemplateStatistic(
            Insight insight,
            UUID templateIdSnapshot,
            String templateNameSnapshot,
            int recordCount,
            double ratio,
            int rank
    ) {
        this.insight = insight;
        this.templateIdSnapshot = templateIdSnapshot;
        this.templateNameSnapshot = templateNameSnapshot;
        this.recordCount = recordCount;
        this.ratio = ratio;
        this.rank = rank;
    }

    public static InsightTemplateStatistic create(
            Insight insight,
            UUID templateIdSnapshot,
            String templateNameSnapshot,
            int recordCount,
            double ratio,
            int rank
    ) {
        return new InsightTemplateStatistic(
                insight,
                templateIdSnapshot,
                templateNameSnapshot,
                recordCount,
                ratio,
                rank
        );
    }
}
