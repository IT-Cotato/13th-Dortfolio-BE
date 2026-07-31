package com.itcotato.dortfolio.domain.activity.entity;

import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.global.entity.BaseEntity;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.ActivityErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "activity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Activity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

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

    private Activity(User user, ActivityType activityType, String title, String description,
                      LocalDate startedAt, LocalDate endedAt, boolean isOngoing) {
        this.user = user;
        this.activityType = activityType;
        this.title = title;
        this.description = description;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.isOngoing = isOngoing;
        this.status = ActivityStatus.IN_PROGRESS;
    }

    public static Activity create(User user, ActivityType activityType, String title, String description,
                                   LocalDate startedAt, LocalDate endedAt, boolean isOngoing) {
        validatePeriod(startedAt, endedAt, isOngoing);
        return new Activity(user, activityType, title, description, startedAt, endedAt, isOngoing);
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

    public boolean isDeleted() {
        return deletedAt != null;
    }

    private static void validatePeriod(LocalDate startedAt, LocalDate endedAt, boolean isOngoing) {
        if (!isOngoing && endedAt == null) {
            throw new CustomException(ActivityErrorCode.END_DATE_REQUIRED);
        }
        if (endedAt != null && startedAt.isAfter(endedAt)) {
            throw new CustomException(ActivityErrorCode.INVALID_ACTIVITY_PERIOD);
        }
    }
}
