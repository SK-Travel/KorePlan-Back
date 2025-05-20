package com.koreplan.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koreplan.data.entity.DataEntity;

public interface DataRepository extends JpaRepository<DataEntity,Long> {

}
