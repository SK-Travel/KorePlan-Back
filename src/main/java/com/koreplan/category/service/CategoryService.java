package com.koreplan.category.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.koreplan.category.dto.ResponseDto;
import com.koreplan.category.dto.CategoryDto;
import com.koreplan.category.entity.CategoryEntity;
import com.koreplan.category.repository.CategoryRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {
	private final CategoryRepository categoryRepository;
	private final ObjectMapper objectMapper;
	@Value("${publicDataKey}")
	private String key;
	
	private String apiUrl = "https://apis.data.go.kr/B551011/KorService2/lclsSystmCode2";
	
	public ResponseEntity<ResponseDto> requestCategoryCodes() throws Exception {
		int rows=1000;
		String fullUrl = apiUrl + "?serviceKey=" + key + "&numOfRows=" + rows 
				+ "&MobileOS=WEB&MobileApp=Koreplan&_type=json&lclsSystmListYn=Y";
		URL url = new URL(fullUrl);

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Content-type", "application/json");
		conn.setConnectTimeout(100000);
		conn.setReadTimeout(100000);
		
		log.info("Response code: " + conn.getResponseCode());

		BufferedReader rd;
		if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
			rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		} else {
			rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
			throw new RuntimeException("API 호출 실패: " + conn.getResponseMessage());
		}
		
		StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        
        rd.close();
        conn.disconnect();
     // 디버깅용: API 응답 로깅
        String responseBody = sb.toString();
        log.debug("API 응답 원본: {}", responseBody);
        
        try {
            // 문자열을 ResponseDto 객체로 변환
            ResponseDto responseDto = objectMapper.readValue(responseBody, ResponseDto.class);
            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            log.error("JSON 변환 중 오류 발생: {}", responseBody, e);
            throw new RuntimeException("JSON 변환 중 오류 발생", e);
        }
    
	}
//	@PostConstruct
//    public void init() {
//        try {
//            ResponseEntity<ResponseDto> response = requestCategoryCodes();
//            ResponseDto dto = response.getBody();
//
//            if (dto == null) {
//                log.warn("API 응답이 null입니다.");
//                return;
//            }
//
//
//            List<CategoryDto> items = dto.getResponse().getBody().getItems().getItem();
//
//            
//            saveCategoryCode(dto);
//            
//            // 여기서 DB 저장 로직 추가 가능
//            // saveAll(items) 등
//
//        } catch (Exception e) {
//            log.error("지역 코드 초기화 중 오류 발생", e);
//        }
//    }
	public void saveCategoryCode(ResponseDto dto) {

        List<CategoryDto> items = dto.getResponse()
                                          .getBody()
                                          .getItems()
                                          .getItem();

        List<CategoryEntity> entities = new ArrayList<>();

        for (CategoryDto item : items) {
        	CategoryEntity categoryEntity = new CategoryEntity();
            categoryEntity.setLclsSystm1Cd(item.getLclsSystm1Cd());
            categoryEntity.setLclsSystm1Nm(item.getLclsSystm1Nm());
            categoryEntity.setLclsSystm2Cd(item.getLclsSystm2Cd());
            categoryEntity.setLclsSystm2Nm(item.getLclsSystm2Nm());
            categoryEntity.setLclsSystm3Cd(item.getLclsSystm3Cd());
            categoryEntity.setLclsSystm3Nm(item.getLclsSystm3Nm());
            categoryEntity.setRnum(item.getRnum());

            entities.add(categoryEntity);
            log.info("Category Entity created: {}", categoryEntity);
        }

        categoryRepository.saveAll(entities);
    }

}
