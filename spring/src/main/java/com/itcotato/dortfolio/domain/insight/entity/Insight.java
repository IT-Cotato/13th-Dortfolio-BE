package com.itcotato.dortfolio.domain.insight.entity;

import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "insights")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Insight extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InsightGenerationStatus status;

    @Column
    private LocalDateTime completedAt;

    @Column
    private LocalDateTime failedAt;

    private Insight(User user) {
        this.user = user;
        this.status = InsightGenerationStatus.PENDING;
    }

    public static Insight pending(User user) {
        return new Insight(user);
    }

    public void complete(LocalDateTime completedAt) {
        this.status = InsightGenerationStatus.COMPLETED;
        this.completedAt = completedAt;
        this.failedAt = null;
    }

    public void fail(LocalDateTime failedAt) {
        this.status = InsightGenerationStatus.FAILED;
        this.completedAt = null;
        this.failedAt = failedAt;
    }
}
