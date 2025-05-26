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

    private String c1Code;
    private String c1Name;
    private String c2Code;
    private String c2Name;
    private String c3Code;
    private String c3Name;
    private int rnum;
}
