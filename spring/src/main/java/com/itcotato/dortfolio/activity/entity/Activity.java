package com.itcotato.dortfolio.activity.entity;

import com.itcotato.dortfolio.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "activity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Activity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // auth 도메인의 User 엔티티가 준비되면 @ManyToOne 연관관계로 교체
    @Column(nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_type_id", nullable = false)
    private ActivityType activityType;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private LocalDate startedAt;

    private LocalDate endedAt;

    @Column(nullable = false)
    private boolean isOngoing;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityStatus status;

    private LocalDateTime archivedAt;

    private LocalDateTime deletedAt;

    private LocalDateTime deletePendingUntil;

    private Activity(UUID userId, ActivityType activityType, String title, String description,
                      LocalDate startedAt, LocalDate endedAt, boolean isOngoing) {
        this.userId = userId;
        this.activityType = activityType;
        this.title = title;
        this.description = description;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.isOngoing = isOngoing;
        this.status = ActivityStatus.IN_PROGRESS;
    }

    public static Activity create(UUID userId, ActivityType activityType, String title, String description,
                                   LocalDate startedAt, LocalDate endedAt, boolean isOngoing) {
        validatePeriod(startedAt, endedAt, isOngoing);
        return new Activity(userId, activityType, title, description, startedAt, endedAt, isOngoing);
    }

    public void update(ActivityType activityType, String title, String description,
                        LocalDate startedAt, LocalDate endedAt, boolean isOngoing) {
        validatePeriod(startedAt, endedAt, isOngoing);
        this.activityType = activityType;
        this.title = title;
        this.description = description;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.isOngoing = isOngoing;
    }

    public void archive() {
        this.status = ActivityStatus.ARCHIVED;
        this.archivedAt = LocalDateTime.now();
    }

    public void markDeleted(int gracePeriodDays) {
        this.deletedAt = LocalDateTime.now();
        this.deletePendingUntil = LocalDateTime.now().plusDays(gracePeriodDays);
    }

    public void restore() {
        this.deletedAt = null;
        this.deletePendingUntil = null;
    }

    private static void validatePeriod(LocalDate startedAt, LocalDate endedAt, boolean isOngoing) {
        if (!isOngoing && endedAt == null) {
            throw new IllegalArgumentException("종료일 미정이 아니면 종료일을 입력해야 합니다.");
        }
        if (endedAt != null && startedAt.isAfter(endedAt)) {
            throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
        }
    }
}
