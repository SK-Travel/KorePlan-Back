package com.koreplan.area.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.koreplan.area.entity.RegionCodeEntity;

public interface RegionCodeRepository extends JpaRepository<RegionCodeEntity,Long> {
    // regioncode 값으로 RegionCodeEntity 조회 (regioncode는 unique해야 함)
    Optional<RegionCodeEntity> findByRegioncode(Long regioncode);
    RegionCodeEntity findByName (String name);
    
    // AI 필터링용: name으로 Optional 조회
    @Query("SELECT r FROM RegionCodeEntity r WHERE r.name = :name")
    Optional<RegionCodeEntity> findRegionByNameForAI(@Param("name") String name);
}
