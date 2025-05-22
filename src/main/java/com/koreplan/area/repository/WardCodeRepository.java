package com.koreplan.area.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koreplan.area.entity.RegionCodeEntity;
import com.koreplan.area.entity.WardCodeEntity;

public interface WardCodeRepository extends JpaRepository <WardCodeEntity,Long> {
	Optional<WardCodeEntity> findByWardcodeAndRegionCodeEntity(Long wardcode, RegionCodeEntity region);
}
