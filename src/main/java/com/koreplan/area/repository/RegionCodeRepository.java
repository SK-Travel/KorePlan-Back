package com.koreplan.area.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koreplan.area.entity.RegionCodeEntity;

public interface RegionCodeRepository extends JpaRepository<RegionCodeEntity,Long> {
	
}
