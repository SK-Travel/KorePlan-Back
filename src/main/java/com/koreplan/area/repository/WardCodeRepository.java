package com.koreplan.area.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koreplan.area.entity.RegionCodeEntity;
import com.koreplan.area.entity.WardCodeEntity;

public interface WardCodeRepository extends JpaRepository <WardCodeEntity,Long> {

    // wardcode 값으로 RegionCodeEntity 조회 (wardcode는 unique해야 함)
    Optional<WardCodeEntity> findByWardcodeAndRegionCodeEntity_Regioncode(Long wardcode, Long regioncode);

}
