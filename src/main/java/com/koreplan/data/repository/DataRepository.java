package com.koreplan.data.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
	// 조회수 높은 순으로 정렬된 테마별 데이터 검색 
	List<DataEntity> findByThemeOrderByViewCountDesc(int theme);
	
	// AI 필터링
	boolean existsByRegionCodeEntityAndWardCodeEntityAndTitle (RegionCodeEntity region, WardCodeEntity ward, String title);
	
    /**
     * 특정 지역(Region)과 동(Ward)에 해당하는 장소 리스트를
     * 장소명(title) 오름차순 정렬하여 반환.
     *
     * @param regionCodeEntity 지역 엔티티
     * @param wardCodeEntity 동 엔티티
     * @return 해당 조건에 맞는 장소 리스트
     */
    List<DataEntity> findByRegionCodeEntityAndWardCodeEntityOrderByTitleAsc( RegionCodeEntity regionCodeEntity, WardCodeEntity wardCodeEntity);
    
    //조회수 증가 메서드
    //데이터 엔티티의 고유 id를 통해 값을 변경시키는 메서드임.
    @Modifying
    @Query("UPDATE DataEntity d SET d.viewCount = d.viewCount + 1 WHERE d.id = :dataId")
    void incrementViewCount(@Param("dataId") Long dataId);
    
    //프론트의 detail페이지에서 사용하기 편하도록 contentId로 찾을 수 있게 메서드 추가함.
    Optional<DataEntity> findByContentId(String contentId);
}
