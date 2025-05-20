package com.koreplan.category.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koreplan.category.entity.CategoryEntity;


public interface CategoryRepository extends JpaRepository<CategoryEntity,Long>{

}
