package com.itcotato.dortfolio.domain.record.entity;

import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "strength_tags")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StrengthTag extends BaseEntity {

	@Column(nullable = false, unique = true, length = 50)
	private String code;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(length = 255)
	private String description;

	private StrengthTag(String code, String name, String description) {
		this.code = code;
		this.name = name;
		this.description = description;
	}

	public static StrengthTag create(String code, String name, String description) {
		return new StrengthTag(code, name, description);
	}
}
