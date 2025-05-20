package com.koreplan.category.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="categories")
@Getter
@Setter
public class CategoryEntity {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String lclsSystm1Cd;
    private String lclsSystm1Nm;
    private String lclsSystm2Cd;
    private String lclsSystm2Nm;
    private String lclsSystm3Cd;
    private String lclsSystm3Nm;
    private int rnum;
}
