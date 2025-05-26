package com.koreplan.category.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koreplan.category.entity.CategoryEntity;


public interface CategoryRepository extends JpaRepository<CategoryEntity,Long>{
	List<CategoryEntity> findByC1Name(String c1Name);
}
