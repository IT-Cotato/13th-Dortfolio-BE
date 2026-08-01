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
        name = "insight_strengths",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_insight_strength_tag",
                columnNames = {"insight_id", "strength_tag_id_snapshot"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InsightStrength extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insight_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Insight insight;

    @Column(name = "strength_tag_id_snapshot", nullable = false)
    private UUID strengthTagIdSnapshot;

    @Column(name = "strength_name_snapshot", nullable = false)
    private String strengthNameSnapshot;

    @Column(nullable = false)
    private int recordCount;

    @Column(nullable = false)
    private double averageScore;

    @Column(nullable = false)
    private double ratio;

    @Column(nullable = false)
    private int rank;

    private InsightStrength(
            Insight insight,
            UUID strengthTagIdSnapshot,
            String strengthNameSnapshot,
            int recordCount,
            double averageScore,
            double ratio,
            int rank
    ) {
        this.insight = insight;
        this.strengthTagIdSnapshot = strengthTagIdSnapshot;
        this.strengthNameSnapshot = strengthNameSnapshot;
        this.recordCount = recordCount;
        this.averageScore = averageScore;
        this.ratio = ratio;
        this.rank = rank;
    }

    public static InsightStrength create(
            Insight insight,
            UUID strengthTagIdSnapshot,
            String strengthNameSnapshot,
            int recordCount,
            double averageScore,
            double ratio,
            int rank
    ) {
        return new InsightStrength(
                insight,
                strengthTagIdSnapshot,
                strengthNameSnapshot,
                recordCount,
                averageScore,
                ratio,
                rank
        );
    }
}
