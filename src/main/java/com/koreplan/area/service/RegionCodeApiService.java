package com.koreplan.area.service;


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
import com.koreplan.area.dto.Item;
import com.koreplan.area.dto.ResponseDto;
import com.koreplan.area.entity.RegionCodeEntity;
import com.koreplan.area.entity.WardCodeEntity;
import com.koreplan.area.repository.RegionCodeRepository;
import com.koreplan.area.repository.WardCodeRepository;
import com.koreplan.category.service.CategoryService;
import com.koreplan.data.service.SaveDataService;
import com.koreplan.service.festival.SaveFestivalService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;



@Slf4j
@Service
@RequiredArgsConstructor
public class RegionCodeApiService {
    private final RegionCodeRepository regionCodeRepository; //시or도 코드 저장
    private final WardCodeRepository wardCodeRepository; // 시or도의 시or구(군) 코드 저장
    private final ObjectMapper objectMapper; // Jackson의 ObjectMapper 주입
    private final CategoryService categoryService;
    private final SaveDataService saveDataService;
    private final SaveFestivalService saveFestivalService;
    @Value("${publicDataKey}")
    private String key;

    private String publicDataKey = "8RzX7q7EJdbZwVX%2BT19pP%2B4eG8Omg8zWmGE6VJQsECN5MNrujoaMTY4FkJww6UeJF1fDoNn6UrkmNycG%2FhCxKw%3D%3D";
    private String apiUrl = "https://apis.data.go.kr/B551011/KorService2/ldongCode2";

    public ResponseEntity<ResponseDto> requestRegionCodes() throws Exception {
    	int rows=100;
        String fullUrl = apiUrl + "?serviceKey=" + publicDataKey +"&numOfRows="+rows +"&MobileOS=WEB&MobileApp=Koreplan&_type=json&lDongListYn=N";
        
        URL url = new URL(fullUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        
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
    
    // 특정 지역 코드에 대한 하위 지역 요청 메소드 추가
    public ResponseEntity<ResponseDto> requestSubRegionCodes(int areaCode) throws Exception {
    	int rows=100;
    	String fullUrl = apiUrl + "?serviceKey=" + publicDataKey 
    		    + "&numOfRows="+rows
    		    + "&pageNo=1"
    		    + "&MobileOS=WEB&MobileApp=Koreplan&_type=json"
    		    + "&lDongRegnCd=" + areaCode;
        URL url = new URL(fullUrl);
        log.info("요청 URL: {}", url);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        
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
        
        String responseBody = sb.toString();
        log.debug("하위 지역 API 응답: {}", responseBody);
        
        ResponseDto responseDto = objectMapper.readValue(responseBody, ResponseDto.class);
        return ResponseEntity.ok(responseDto);
    }
    // 주석 처리 해야 함.

//    @PostConstruct
//    public void init() {
//         saveAllDatas();
//         categoryService.savecategory();
//         saveDataService.saveDataService();
//         saveFestivalService.saveFestival();
//         
//    }
    public void saveRegionCode(ResponseDto dto) throws Exception {
        List<Item> items = dto.getResponse().getBody().getItems().getItem();
        
        for (Item item : items) {
            // 1. 먼저 Region 엔티티 생성 및 저장
            RegionCodeEntity regionEntity = new RegionCodeEntity();
            regionEntity.setRegioncode(Long.valueOf(item.getCode()));
            regionEntity.setName(item.getName());
            regionEntity.setWardList(new ArrayList<>());
            
            // 2. Region 엔티티를 저장하여 ID 생성 및 영속화
            RegionCodeEntity savedRegion = regionCodeRepository.save(regionEntity);
            
            // 3. 이후 Ward 데이터 요청 및 처리
            ResponseEntity<ResponseDto> wardResponse = requestSubRegionCodes(item.getCode());
            if (wardResponse != null && wardResponse.getBody() != null) {
                saveWardCode(wardResponse.getBody(), savedRegion);
            }
        }
    }

    public void saveWardCode(ResponseDto dto, RegionCodeEntity regionEntity) {
        
        List<Item> items = dto.getResponse().getBody().getItems().getItem();
        List<WardCodeEntity> entities = new ArrayList<>();
        
        for (Item item : items) {
            WardCodeEntity wardEntity = new WardCodeEntity();
            wardEntity.setWardcode(Long.valueOf(item.getCode()));
            wardEntity.setName(item.getName());
            wardEntity.setRegionCodeEntity(regionEntity); // 이미 저장된 Region 엔티티 참조
            entities.add(wardEntity);
            log.info("Ward Entity created: {}", wardEntity);
        }
        
        // Ward 엔티티들 저장
        wardCodeRepository.saveAll(entities);
    }
    private void saveAllDatas() {
    	try {
            ResponseEntity<ResponseDto> response = requestRegionCodes();
            ResponseDto dto = response.getBody();

            if (dto == null) {
                log.warn("API 응답이 null입니다.");
                return;
            }


            List<Item> items = dto.getResponse().getBody().getItems().getItem();

            // 예시: 아이템 리스트 출력
            for (Item item : items) {
                log.info("####지역 코드: {}, 지역명: {}", item.getCode(), item.getName());
            }
            saveRegionCode(dto);

            // 여기서 DB 저장 로직 추가 가능
            // saveAll(items) 등

        }
    	catch (Exception e) {
            log.error("지역 코드 초기화 중 오류 발생", e);
        }
    }
    
  
    	
}

