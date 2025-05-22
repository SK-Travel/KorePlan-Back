package com.koreplan.area.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "Ward",
	uniqueConstraints = @UniqueConstraint(columnNames = {"wardcode", "region"})) // 이렇게 하면 wardcode와 region의 조합이 유일해야 하므로, 같은 wardcode라도 다른 region이면 허용.
@Getter
@Setter

public class WardCodeEntity {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	private Long wardcode;
	
	private String name;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "region")
	private RegionCodeEntity regionCodeEntity;
}
