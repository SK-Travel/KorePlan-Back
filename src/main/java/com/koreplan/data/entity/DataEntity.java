package com.koreplan.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "data")
@Getter
@Setter
public class DataEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String contentId;

	private String addr1;

	@Column(nullable = true)
	private String addr2;

	private String regioncode;

	private String wardcode;

	private String mapx;

	private String mapy;

	private String title;

	private String C1Code;

	private String C2Code;

	private String C3Code;

	private String firstimage;

	private String firstimage2;
	@Column(nullable = true)
	private String tel;
}
