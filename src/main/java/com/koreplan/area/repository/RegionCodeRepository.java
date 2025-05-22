package com.koreplan.area.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koreplan.area.entity.RegionCodeEntity;

public interface RegionCodeRepository extends JpaRepository<RegionCodeEntity,Long> {
	 Optional<RegionCodeEntity> findByRegioncode(Long regioncode);
}
