// FestivalRepository.java에 추가해야 할 메서드들

package com.koreplan.repository.festival;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.koreplan.entity.festival.FestivalEntity;

@Repository
public interface FestivalRepository extends JpaRepository<FestivalEntity, String> {
    
    // C2Code로 축제 조회
    List<FestivalEntity> findByC2Code(String c2Code);
    
    // 지역 코드로 축제 조회
    List<FestivalEntity> findByRegionCodeEntity_Regioncode(Long regioncode);
    
    // contentId로 존재 여부 확인 (중복 체크용 - SaveFestivalService에서 사용)
    boolean existsByContentId(String contentId);
    
    // 제목으로 검색 (대소문자 무시, 부분 일치) - 옵션으로 추가 가능
    // List<FestivalEntity> findByTitleContainingIgnoreCase(String title);
    
    // 조회수 기준 내림차순 정렬 - 옵션으로 추가 가능
    // List<FestivalEntity> findAllByOrderByViewCountDesc();
}