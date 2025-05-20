package com.koreplan.area.entity;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "region")
@Getter
@Setter
public class RegionCodeEntity {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	private Long regioncode;
	
	private String name;
	
	@OneToMany(mappedBy = "regionCodeEntity", cascade = CascadeType.ALL)
	private List<WardCodeEntity> wardList;
	
	
}
