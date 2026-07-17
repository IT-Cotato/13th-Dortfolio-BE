package com.itcotato.dortfolio.activity.entity;

import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "activity_type")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityType extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean isDefault;

    private ActivityType(User user, String name, boolean isDefault) {
        this.user = user;
        this.name = name;
        this.isDefault = isDefault;
    }

    public static ActivityType create(User user, String name) {
        return new ActivityType(user, name, false);
    }

    public static ActivityType createDefault(User user, String name) {
        return new ActivityType(user, name, true);
    }
}
