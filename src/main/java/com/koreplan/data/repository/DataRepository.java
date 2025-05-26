package com.koreplan.data.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koreplan.data.entity.DataEntity;
import com.koreplan.area.entity.RegionCodeEntity;
import com.koreplan.area.entity.WardCodeEntity;



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
}
