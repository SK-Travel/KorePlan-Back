package com.koreplan.repository.theme;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koreplan.entity.theme.ThemeEntity;

public interface ThemeRepository extends JpaRepository<ThemeEntity, Integer> {

	// ThemeName으로 검색
    Optional<ThemeEntity> findByThemeName(String themeName);
    
    // ThemeName에 특정 문자가 포함된 것들 검색
    List<ThemeEntity> findByThemeNameContaining(String keyword);
    
    // 모든 테마를 ThemeName 순으로 정렬해서 가져오기
    List<ThemeEntity> findAllByOrderByThemeNameAsc();
    
    // contentTypeId로 존재 여부 확인
    boolean existsByContentTypeId(Integer contentTypeId);
	
}
