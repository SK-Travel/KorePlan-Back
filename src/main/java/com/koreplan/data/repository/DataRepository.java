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
	
	// ✅ 존재 여부 확인 (단일 제목 + themeId 리스트 기준)
	@Query("SELECT COUNT(d) > 0 FROM DataEntity d " + "WHERE d.regionCodeEntity = :region " + "AND d.wardCodeEntity = :ward " + "AND d.title = :title " + "AND d.theme IN :themeIds")
	boolean existsByRegionCodeEntityAndWardCodeEntityAndTitle (@Param("region") RegionCodeEntity region,
		    @Param("ward") WardCodeEntity ward,
		    @Param("title") String title,
		    @Param("themeIds") List<Integer> themeIds);
	
    // --- 지역 + 테마로 조회 (대소문자 무시X, 공백 무시X) ---
    @Query("SELECT d FROM DataEntity d " +  "WHERE d.regionCodeEntity = :region " + "AND d.wardCodeEntity = :ward " + "AND d.theme IN :themeIds")
    List<DataEntity> findByRegionCodeEntityAndWardCodeEntityAndThemeIn (@Param("region") RegionCodeEntity region,
        @Param("ward") WardCodeEntity ward,
        @Param("themeIds") List<Integer> themeIds);
    
    // 숙소 하나 추가하기
    // DataEntity
    List<DataEntity> findByRegionCodeEntityAndWardCodeEntityAndC1CodeOrderByViewCountDesc(RegionCodeEntity region, WardCodeEntity ward, String c1Code);
    

    //조회수 증가 메서드
    //데이터 엔티티의 고유 id를 통해 값을 변경시키는 메서드임.
    @Modifying
    @Query("UPDATE DataEntity d SET d.viewCount = d.viewCount + 1 WHERE d.id = :dataId")
    void incrementViewCount(@Param("dataId") Long dataId);
    
    //프론트의 detail페이지에서 사용하기 편하도록 contentId로 찾을 수 있게 메서드 추가함.
    Optional<DataEntity> findByContentId(String contentId);
    //이미지 서비스에서 사용하는 컨텐트아이디가 실재하는지 여부 확인하는 메서드(이미지를 가져올 때 유효한 z컨텐트아이디인가)
	boolean existsByContentId(String contentId);
	//여러 컨텐츠아이디로 조회하는 기능
	List<DataEntity> findByContentIdIn(List<String> contentIds);
}
