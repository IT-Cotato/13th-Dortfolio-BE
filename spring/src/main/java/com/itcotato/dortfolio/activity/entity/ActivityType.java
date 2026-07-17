package com.itcotato.dortfolio.activity.entity;

import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "activity_type")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityType extends BaseEntity {

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean isDefault;

    private ActivityType(UUID userId, String name, boolean isDefault) {
        this.userId = userId;
        this.name = name;
        this.isDefault = isDefault;
    }

    public static ActivityType create(UUID userId, String name) {
        return new ActivityType(userId, name, false);
    }

    public static ActivityType createDefault(UUID userId, String name) {
        return new ActivityType(userId, name, true);
    }
}
