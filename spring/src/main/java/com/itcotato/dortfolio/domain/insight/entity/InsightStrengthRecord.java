package com.itcotato.dortfolio.domain.insight.entity;

import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "insight_strength_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_insight_strength_record",
                columnNames = {"insight_strength_id", "record_id_snapshot"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InsightStrengthRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insight_strength_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private InsightStrength insightStrength;

    @Column(name = "record_id_snapshot", nullable = false)
    private UUID recordIdSnapshot;

    @Column(name = "record_title_snapshot", nullable = false)
    private String recordTitleSnapshot;

    @Column(name = "record_completed_at_snapshot", nullable = false)
    private LocalDateTime recordCompletedAtSnapshot;

    private InsightStrengthRecord(
            InsightStrength insightStrength,
            UUID recordIdSnapshot,
            String recordTitleSnapshot,
            LocalDateTime recordCompletedAtSnapshot
    ) {
        this.insightStrength = insightStrength;
        this.recordIdSnapshot = recordIdSnapshot;
        this.recordTitleSnapshot = recordTitleSnapshot;
        this.recordCompletedAtSnapshot = recordCompletedAtSnapshot;
    }

    public static InsightStrengthRecord create(
            InsightStrength insightStrength,
            UUID recordIdSnapshot,
            String recordTitleSnapshot,
            LocalDateTime recordCompletedAtSnapshot
    ) {
        return new InsightStrengthRecord(
                insightStrength,
                recordIdSnapshot,
                recordTitleSnapshot,
                recordCompletedAtSnapshot
        );
    }
}
