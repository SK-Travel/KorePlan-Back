package com.koreplan.data.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // 새로 추가할 메서드 (score 정렬 + 페이징)
    @Query("SELECT d FROM DataEntity d " + 
           "WHERE d.regionCodeEntity = :region " + 
           "AND d.wardCodeEntity = :ward " + 
           "AND d.theme IN :themeIds " +
           "ORDER BY d.score DESC")
    Page<DataEntity> findByRegionCodeEntityAndWardCodeEntityAndThemeIn(
        @Param("region") RegionCodeEntity region,
        @Param("ward") WardCodeEntity ward,
        @Param("themeIds") List<Integer> themeIds,
        Pageable pageable
    );
    
    @Query("SELECT d FROM DataEntity d " +
    	       "WHERE d.regionCodeEntity = :region " +
    	       "AND d.theme IN :themeIds " +
    	       "ORDER BY d.score DESC")
    	Page<DataEntity> findByRegionCodeEntityAndThemeIn(
    	    @Param("region") RegionCodeEntity region,
    	    @Param("themeIds") List<Integer> themeIds,
    	    Pageable pageable
    	);
    // 숙소 하나 추가하기
    // DataEntity
    List<DataEntity> findByRegionCodeEntityAndWardCodeEntityAndC1CodeOrderByViewCountDesc(RegionCodeEntity region, WardCodeEntity ward, String c1Code);
    

    //조회수 증가 메서드
    //데이터 엔티티의 고유 id를 통해 값을 변경시키는 메서드임.
    @Modifying
    @Query("UPDATE DataEntity d SET d.viewCount = d.viewCount + 1 WHERE d.id = :dataId")
    void incrementViewCount(@Param("dataId") Long dataId);
    //좋아요수 증가 메서드
    @Modifying
    @Query("UPDATE DataEntity d SET d.likeCount = d.likeCount + 1 WHERE d.id = :dataId")
    void incrementLikeCount(@Param("dataId") Long dataId);
    //종아요수 감소 메서드
    @Modifying  
    @Query("UPDATE DataEntity d SET d.likeCount = d.likeCount - 1 WHERE d.id = :dataId")
    void decrementLikeCount(@Param("dataId") Long dataId);
    
    //프론트의 detail페이지에서 사용하기 편하도록 contentId로 찾을 수 있게 메서드 추가함.
    Optional<DataEntity> findByContentId(String contentId);
    //이미지 서비스에서 사용하는 컨텐트아이디가 실재하는지 여부 확인하는 메서드(이미지를 가져올 때 유효한 컨텐트아이디인가)
	boolean existsByContentId(String contentId);
	//여러 컨텐츠아이디로 조회하는 기능
	List<DataEntity> findByContentIdIn(List<String> contentIds);
	

	//상위 5개 통합점수로 조회
	List<DataEntity> findTop5ByOrderByScoreDesc();
	//지역 테마 리스트에서 사용할 메서드
	List<DataEntity> findByThemeOrderByLikeCountDesc(int themeNum);
	List<DataEntity> findByThemeOrderByScoreDesc(int themeNum);
	List<DataEntity> findByThemeOrderByRatingDesc(int themeNum);
	List<DataEntity> findByThemeOrderByReviewCountDesc(int themeNum);
	//검색에서 사용할 메서드
	List<DataEntity> findAllByOrderByScoreDesc();
	List<DataEntity> findAllByOrderByViewCountDesc();
	List<DataEntity> findAllByOrderByLikeCountDesc();
	List<DataEntity>findAllByOrderByRatingDesc();
	List<DataEntity> findAllByOrderByReviewCountDesc();
	
	// ================= 기존 비페이징 메서드들 (다른 기능에서 사용 중이므로 유지) =================
	
	// ✅ 테마별 + 정렬 + JOIN FETCH 메서드들 (LazyInitializationException 해결)
	@Query("SELECT d FROM DataEntity d " +
           "LEFT JOIN FETCH d.regionCodeEntity " +
           "LEFT JOIN FETCH d.wardCodeEntity " +
           "WHERE d.theme = :theme " +
           "ORDER BY d.score DESC")
    List<DataEntity> findByThemeOrderByScoreDescWithRegion(@Param("theme") int theme);
    
    @Query("SELECT d FROM DataEntity d " +
           "LEFT JOIN FETCH d.regionCodeEntity " +
           "LEFT JOIN FETCH d.wardCodeEntity " +
           "WHERE d.theme = :theme " +
           "ORDER BY d.viewCount DESC")
    List<DataEntity> findByThemeOrderByViewCountDescWithRegion(@Param("theme") int theme);
    
    @Query("SELECT d FROM DataEntity d " +
           "LEFT JOIN FETCH d.regionCodeEntity " +
           "LEFT JOIN FETCH d.wardCodeEntity " +
           "WHERE d.theme = :theme " +
           "ORDER BY d.likeCount DESC")
    List<DataEntity> findByThemeOrderByLikeCountDescWithRegion(@Param("theme") int theme);
    
    @Query("SELECT d FROM DataEntity d " +
           "LEFT JOIN FETCH d.regionCodeEntity " +
           "LEFT JOIN FETCH d.wardCodeEntity " +
           "WHERE d.theme = :theme " +
           "ORDER BY d.rating DESC")
    List<DataEntity> findByThemeOrderByRatingDescWithRegion(@Param("theme") int theme);
    
    @Query("SELECT d FROM DataEntity d " +
           "LEFT JOIN FETCH d.regionCodeEntity " +
           "LEFT JOIN FETCH d.wardCodeEntity " +
           "WHERE d.theme = :theme " +
           "ORDER BY d.reviewCount DESC")
    List<DataEntity> findByThemeOrderByReviewCountDescWithRegion(@Param("theme") int theme);
    
    // ✅ 전체 데이터 정렬 + JOIN FETCH 메서드들
    @Query("SELECT d FROM DataEntity d " +
           "LEFT JOIN FETCH d.regionCodeEntity " +
           "LEFT JOIN FETCH d.wardCodeEntity " +
           "ORDER BY d.score DESC")
    List<DataEntity> findAllByOrderByScoreDescWithRegion();
    
    @Query("SELECT d FROM DataEntity d " +
           "LEFT JOIN FETCH d.regionCodeEntity " +
           "LEFT JOIN FETCH d.wardCodeEntity " +
           "ORDER BY d.viewCount DESC")
    List<DataEntity> findAllByOrderByViewCountDescWithRegion();
    
    @Query("SELECT d FROM DataEntity d " +
           "LEFT JOIN FETCH d.regionCodeEntity " +
           "LEFT JOIN FETCH d.wardCodeEntity " +
           "ORDER BY d.likeCount DESC")
    List<DataEntity> findAllByOrderByLikeCountDescWithRegion();
    
    @Query("SELECT d FROM DataEntity d " +
           "LEFT JOIN FETCH d.regionCodeEntity " +
           "LEFT JOIN FETCH d.wardCodeEntity " +
           "ORDER BY d.rating DESC")
    List<DataEntity> findAllByOrderByRatingDescWithRegion();
    
    @Query("SELECT d FROM DataEntity d " +
           "LEFT JOIN FETCH d.regionCodeEntity " +
           "LEFT JOIN FETCH d.wardCodeEntity " +
           "ORDER BY d.reviewCount DESC")
    List<DataEntity> findAllByOrderByReviewCountDescWithRegion();
    
    // Top5Place용 - 숙박(AC) 제외하고 상위 5개 조회
    @Query("SELECT d FROM DataEntity d " +
           "LEFT JOIN FETCH d.regionCodeEntity " +
           "LEFT JOIN FETCH d.wardCodeEntity " +
           "WHERE d.c1Code != 'AC' " +
           "ORDER BY d.score DESC " +
           "LIMIT 5")
    List<DataEntity> findTop5ByOrderByScoreDescExcludingAccommodation();

    // Top5Hotel용 - 숙박(AC)만 상위 5개 조회  
    @Query("SELECT d FROM DataEntity d " +
           "LEFT JOIN FETCH d.regionCodeEntity " +
           "LEFT JOIN FETCH d.wardCodeEntity " +
           "WHERE d.c1Code = 'AC' " +
           "ORDER BY d.score DESC " +
           "LIMIT 5")
    List<DataEntity> findTop5ByC1CodeOrderByScoreDesc();

    // ================= 페이징 지원 메서드들 (신규 추가) =================
    
 // ✅ 1. 전국 + 테마 + 정렬 + 페이징 (테마는 반드시 포함!)
    @Query("SELECT d FROM DataEntity d " +
           "LEFT JOIN FETCH d.regionCodeEntity " +
           "LEFT JOIN FETCH d.wardCodeEntity " +
           "WHERE d.theme = :theme " +
           "ORDER BY d.score DESC")
    Page<DataEntity> findByThemeOrderByScoreDescWithRegionPaged(@Param("theme") int theme, Pageable pageable);

    @Query("SELECT d FROM DataEntity d " +
           "LEFT JOIN FETCH d.regionCodeEntity " +
           "LEFT JOIN FETCH d.wardCodeEntity " +
           "WHERE d.theme = :theme " +
           "ORDER BY d.viewCount DESC")
    Page<DataEntity> findByThemeOrderByViewCountDescWithRegionPaged(@Param("theme") int theme, Pageable pageable);

    @Query("SELECT d FROM DataEntity d " +
           "LEFT JOIN FETCH d.regionCodeEntity " +
           "LEFT JOIN FETCH d.wardCodeEntity " +
           "WHERE d.theme = :theme " +
           "ORDER BY d.likeCount DESC")
    Page<DataEntity> findByThemeOrderByLikeCountDescWithRegionPaged(@Param("theme") int theme, Pageable pageable);

    @Query("SELECT d FROM DataEntity d " +
           "LEFT JOIN FETCH d.regionCodeEntity " +
           "LEFT JOIN FETCH d.wardCodeEntity " +
           "WHERE d.theme = :theme " +
           "ORDER BY d.rating DESC")
    Page<DataEntity> findByThemeOrderByRatingDescWithRegionPaged(@Param("theme") int theme, Pageable pageable);

    @Query("SELECT d FROM DataEntity d " +
           "LEFT JOIN FETCH d.regionCodeEntity " +
           "LEFT JOIN FETCH d.wardCodeEntity " +
           "WHERE d.theme = :theme " +
           "ORDER BY d.reviewCount DESC")
    Page<DataEntity> findByThemeOrderByReviewCountDescWithRegionPaged(@Param("theme") int theme, Pageable pageable);

    // ✅ 2. 특정 지역 + 테마 + 정렬 + 페이징
    @Query("SELECT d FROM DataEntity d " +
           "LEFT JOIN FETCH d.regionCodeEntity " +
           "LEFT JOIN FETCH d.wardCodeEntity " +
           "WHERE d.theme = :theme " +
           "AND d.regionCodeEntity.regioncode = :regionCode " +
           "ORDER BY d.score DESC")
    Page<DataEntity> findByThemeAndRegionOrderByScoreDescWithRegionPaged(
        @Param("theme") int theme, 
        @Param("regionCode") Long regionCode, 
        Pageable pageable);
        
    @Query("SELECT d FROM DataEntity d " +
           "LEFT JOIN FETCH d.regionCodeEntity " +
           "LEFT JOIN FETCH d.wardCodeEntity " +
           "WHERE d.theme = :theme " +
           "AND d.regionCodeEntity.regioncode = :regionCode " +
           "ORDER BY d.viewCount DESC")
    Page<DataEntity> findByThemeAndRegionOrderByViewCountDescWithRegionPaged(
        @Param("theme") int theme, 
        @Param("regionCode") Long regionCode, 
        Pageable pageable);
        
    @Query("SELECT d FROM DataEntity d " +
           "LEFT JOIN FETCH d.regionCodeEntity " +
           "LEFT JOIN FETCH d.wardCodeEntity " +
           "WHERE d.theme = :theme " +
           "AND d.regionCodeEntity.regioncode = :regionCode " +
           "ORDER BY d.likeCount DESC")
    Page<DataEntity> findByThemeAndRegionOrderByLikeCountDescWithRegionPaged(
        @Param("theme") int theme, 
        @Param("regionCode") Long regionCode, 
        Pageable pageable);
        
    @Query("SELECT d FROM DataEntity d " +
           "LEFT JOIN FETCH d.regionCodeEntity " +
           "LEFT JOIN FETCH d.wardCodeEntity " +
           "WHERE d.theme = :theme " +
           "AND d.regionCodeEntity.regioncode = :regionCode " +
           "ORDER BY d.rating DESC")
    Page<DataEntity> findByThemeAndRegionOrderByRatingDescWithRegionPaged(
        @Param("theme") int theme, 
        @Param("regionCode") Long regionCode, 
        Pageable pageable);
        
    @Query("SELECT d FROM DataEntity d " +
           "LEFT JOIN FETCH d.regionCodeEntity " +
           "LEFT JOIN FETCH d.wardCodeEntity " +
           "WHERE d.theme = :theme " +
           "AND d.regionCodeEntity.regioncode = :regionCode " +
           "ORDER BY d.reviewCount DESC")
    Page<DataEntity> findByThemeAndRegionOrderByReviewCountDescWithRegionPaged(
        @Param("theme") int theme, 
        @Param("regionCode") Long regionCode, 
        Pageable pageable);

    // ✅ 3. 특정 지역 + 구/군 + 테마 + 정렬 + 페이징
    @Query("SELECT d FROM DataEntity d " +
           "LEFT JOIN FETCH d.regionCodeEntity " +
           "LEFT JOIN FETCH d.wardCodeEntity " +
           "WHERE d.theme = :theme " +
           "AND d.regionCodeEntity.regioncode = :regionCode " +
           "AND d.wardCodeEntity.wardcode IN :wardCodes " +
           "ORDER BY d.score DESC")
    Page<DataEntity> findByThemeAndRegionAndWardsOrderByScoreDescWithRegionPaged(
        @Param("theme") int theme, 
        @Param("regionCode") Long regionCode, 
        @Param("wardCodes") List<Long> wardCodes, 
        Pageable pageable);
        
    @Query("SELECT d FROM DataEntity d " +
           "LEFT JOIN FETCH d.regionCodeEntity " +
           "LEFT JOIN FETCH d.wardCodeEntity " +
           "WHERE d.theme = :theme " +
           "AND d.regionCodeEntity.regioncode = :regionCode " +
           "AND d.wardCodeEntity.wardcode IN :wardCodes " +
           "ORDER BY d.viewCount DESC")
    Page<DataEntity> findByThemeAndRegionAndWardsOrderByViewCountDescWithRegionPaged(
        @Param("theme") int theme, 
        @Param("regionCode") Long regionCode, 
        @Param("wardCodes") List<Long> wardCodes, 
        Pageable pageable);
        
    @Query("SELECT d FROM DataEntity d " +
           "LEFT JOIN FETCH d.regionCodeEntity " +
           "LEFT JOIN FETCH d.wardCodeEntity " +
           "WHERE d.theme = :theme " +
           "AND d.regionCodeEntity.regioncode = :regionCode " +
           "AND d.wardCodeEntity.wardcode IN :wardCodes " +
           "ORDER BY d.likeCount DESC")
    Page<DataEntity> findByThemeAndRegionAndWardsOrderByLikeCountDescWithRegionPaged(
        @Param("theme") int theme, 
        @Param("regionCode") Long regionCode, 
        @Param("wardCodes") List<Long> wardCodes, 
        Pageable pageable);
        
    @Query("SELECT d FROM DataEntity d " +
           "LEFT JOIN FETCH d.regionCodeEntity " +
           "LEFT JOIN FETCH d.wardCodeEntity " +
           "WHERE d.theme = :theme " +
           "AND d.regionCodeEntity.regioncode = :regionCode " +
           "AND d.wardCodeEntity.wardcode IN :wardCodes " +
           "ORDER BY d.rating DESC")
    Page<DataEntity> findByThemeAndRegionAndWardsOrderByRatingDescWithRegionPaged(
        @Param("theme") int theme, 
        @Param("regionCode") Long regionCode, 
        @Param("wardCodes") List<Long> wardCodes, 
        Pageable pageable);
        
    @Query("SELECT d FROM DataEntity d " +
           "LEFT JOIN FETCH d.regionCodeEntity " +
           "LEFT JOIN FETCH d.wardCodeEntity " +
           "WHERE d.theme = :theme " +
           "AND d.regionCodeEntity.regioncode = :regionCode " +
           "AND d.wardCodeEntity.wardcode IN :wardCodes " +
           "ORDER BY d.reviewCount DESC")
    Page<DataEntity> findByThemeAndRegionAndWardsOrderByReviewCountDescWithRegionPaged(
        @Param("theme") int theme, 
        @Param("regionCode") Long regionCode, 
        @Param("wardCodes") List<Long> wardCodes, 
        Pageable pageable);
}