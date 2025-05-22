package com.koreplan.area.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koreplan.area.entity.RegionCodeEntity;

public interface RegionCodeRepository extends JpaRepository<RegionCodeEntity,Long> {
    // regioncode 값으로 RegionCodeEntity 조회 (regioncode는 unique해야 함)
    Optional<RegionCodeEntity> findByRegioncode(Long regioncode);
}
