package com.koreplan.data.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koreplan.data.entity.DataEntity;

public interface DataRepository extends JpaRepository<DataEntity,Long> {
	List<DataEntity> findByC1Code(String c1Code);
}
