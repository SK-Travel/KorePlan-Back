package com.koreplan.service.theme;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.koreplan.entity.theme.ThemeEntity;
import com.koreplan.repository.theme.ThemeRepository;

@Service
public class ThemeService {
	@Autowired
	private ThemeRepository themeRepository;
	
	
    
    // 모든 테마 가져오기
    public List<ThemeEntity> getAllThemes() {
        return themeRepository.findAll();
    }
    
    // ID로 테마 찾기
    public ThemeEntity getThemeById(Integer contentTypeId) {
        return themeRepository.findById(contentTypeId)
                .orElseThrow(() -> new RuntimeException("Theme not found with id: " + contentTypeId));
    }
    
    // 테마명으로 찾기
    public ThemeEntity getThemeByName(String themeName) {
        return themeRepository.findByThemeName(themeName)
                .orElseThrow(() -> new RuntimeException("Theme not found with name: " + themeName));
    }
    
    // 키워드가 포함된 테마들 검색
    public List<ThemeEntity> searchThemes(String keyword) {
        return themeRepository.findByThemeNameContaining(keyword);
    }
    
    // 정렬된 테마 목록
    public List<ThemeEntity> getThemesSorted() {
        return themeRepository.findAllByOrderByThemeNameAsc();
    }
}
