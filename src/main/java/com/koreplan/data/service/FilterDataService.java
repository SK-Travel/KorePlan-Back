package com.koreplan.data.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.koreplan.area.repository.RegionCodeRepository;
import com.koreplan.area.repository.WardCodeRepository;
import com.koreplan.category.entity.CategoryEntity;
import com.koreplan.category.repository.CategoryRepository;
import com.koreplan.data.entity.DataEntity;
import com.koreplan.data.repository.DataRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FilterDataService {
	private final RegionCodeRepository regionCodeRepository;
	private final WardCodeRepository wardCodeRepository;
	private final CategoryRepository categoryRepository;
	private final DataRepository dataRepository;
	
	public List<DataEntity> findCatergoryCode(String categoryname) {
	    log.info("메서드 시작 - categoryname: {}", categoryname);
	    
	    String categoryname_ = "음식";
	    log.info("검색할 카테고리명: {}", categoryname_);
	    
	    // 전체 카테고리 조회
	    List<CategoryEntity> categories = categoryRepository.findByC1Name(categoryname_);
	    log.info("조회된 카테고리 개수: {}", categories.size());
	    
	    if (categories.isEmpty()) {
	        log.warn("카테고리를 찾을 수 없음: {}", categoryname_);
	        return List.of();
	    }
	  
	    // 각 카테고리에 대해 최상위 카테고리를 조회
	    CategoryEntity category = categories.get(0); 
	    
        log.info("처리 중인 카테고리: {}, 코드: {}", category.getC1Name(), category.getC1Code());
        
        String categorycode = category.getC1Code();
        List<DataEntity> results = dataRepository.findByC1Code(categorycode);
        
        log.info("카테고리 코드 {} 에 대한 결과 개수: {}", categorycode, results.size());
	  
	    return results;
	}
	
	@PostConstruct
	public void init() {
		String a ="1";
		List<DataEntity> abc = findCatergoryCode(a);
	}
	
	
	
}
