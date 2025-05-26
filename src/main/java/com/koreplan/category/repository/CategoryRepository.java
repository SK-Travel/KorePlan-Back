package com.koreplan.category.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koreplan.category.entity.CategoryEntity;
import java.util.List;



public interface CategoryRepository extends JpaRepository<CategoryEntity,Long>{
	//String findC1CodeByC1Name(String C1Name);
	List<CategoryEntity> findByC1Name(String c1Name);//대분류 이름 받았을때 해당 대분류 코드 찾기
	List<CategoryEntity> findByC2Name(String c2Name);//중분류 이름 받았을때 해당 중분류 코드 찾기
	CategoryEntity findByC3Name(String c3Name);//소분류 이름 받았을때 해당 소분류 코드 찾기
}
