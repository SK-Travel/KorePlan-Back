package com.koreplan.data.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.koreplan.area.entity.RegionCodeEntity;
import com.koreplan.area.entity.WardCodeEntity;
import com.koreplan.data.entity.DataEntity;



public interface DataRepository extends JpaRepository<DataEntity,Long> {
	//-----------------카테고리별 데이터 탐색------------//
	List<DataEntity> findByC1Code(String C1Code);
	List<DataEntity> findByC2Code(String C2Code);
	List<DataEntity> findByC3Code(String C3Code);
	//-----------------------------------------------
	
	//------------------지역별 데이터 탐색 -----------------//
	List<DataEntity> findByRegionCodeEntity(RegionCodeEntity regionCodeEntity);
	List<DataEntity> findByWardCodeEntity(WardCodeEntity wardCodeEntity);
	//-----------------------------------------------------------//
	
	//------------------Theme 별 데이터 검색---------------------//
	//관광타입(12:관광지, 14:문화시설, 15:축제공연행사, 25:여행코스, 28:레포츠, 32:숙박, 38:쇼핑, 39:음식점) ID
	List<DataEntity> findByTheme(int theme);
	
	// AI 필터링
	boolean existsByRegionCodeEntityAndWardCodeEntityAndTitle (RegionCodeEntity region, WardCodeEntity ward, String title);
	
	// 대소문자 무시 + 공백 무시
	@Query("SELECT d FROM DataEntity d " + "WHERE d.regionCodeEntity = :region " + "AND d.wardCodeEntity = :ward")
    List<DataEntity> findByRegionCodeEntityAndWardCodeEntity(@Param("region") RegionCodeEntity region,
        @Param("ward") WardCodeEntity ward);
}
