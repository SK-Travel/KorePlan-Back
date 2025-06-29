package com.koreplan.area.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.koreplan.area.entity.RegionCodeEntity;
import com.koreplan.area.entity.WardCodeEntity;

public interface WardCodeRepository extends JpaRepository <WardCodeEntity,Long> {

    // wardcode 값으로 RegionCodeEntity 조회 (wardcode는 unique해야 함)
    Optional<WardCodeEntity> findByWardcodeAndRegionCodeEntity_Regioncode(Long wardcode, Long regioncode);

	WardCodeEntity findByName(String wardName);
	 @Query("SELECT w FROM WardCodeEntity w WHERE w.regionCodeEntity.name = :regionName AND w.name = :wardName")
	    Optional<WardCodeEntity> findByRegionNameAndWardName(
	        @Param("regionName") String regionName, 
	        @Param("wardName") String wardName);
	 
    // AI 필터링용: name + region 으로 유일하게 조회
    @Query("SELECT w FROM WardCodeEntity w WHERE w.name = :name AND w.regionCodeEntity = :region")
    Optional<WardCodeEntity> findWardByNameAndRegionForAI(@Param("name") String name, @Param("region") RegionCodeEntity region);
    
    List<WardCodeEntity> findByNameStartingWithAndRegionCodeEntity(String namePrefix, RegionCodeEntity regionCodeEntity);

}
