package com.itcotato.dortfolio.activity.entity;

import com.itcotato.dortfolio.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "activity_type")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityType extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

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
