package com.koreplan.service.detail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koreplan.dto.detail.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
@RequiredArgsConstructor
@Service
@Slf4j
public class DetailInfoService {
    
    @Value("${publicDataKey}")
    private String serviceKey;
    
    private final String API_BASE_URL = "https://apis.data.go.kr/B551011/KorService2/detailIntro2";
    private final ObjectMapper objectMapper;
    
    
    
    /**
     * contentId와 contentTypeId에 따른 상세 정보 조회
     */
    public ResponseEntity<Object> getDetailIntro(String contentId, String contentTypeId) throws Exception {
        log.info("상세정보 조회 시작 - contentId: {}, contentTypeId: {}", contentId, contentTypeId);
        
        // API URL 구성
        String fullUrl = buildApiUrl(contentId, contentTypeId);
        log.info("API 호출 URL: {}", fullUrl);
        
        // HTTP 연결 설정
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
            // contentTypeId에 따라 적절한 DTO로 변환
            Object responseDto = parseResponse(responseBody, contentTypeId);
            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            log.error("JSON 변환 중 오류 발생: {}", responseBody, e);
            throw new RuntimeException("JSON 변환 중 오류 발생", e);
        }
    }
    
    /**
     * API URL 구성
     */
    private String buildApiUrl(String contentId, String contentTypeId) {
        return API_BASE_URL + 
               "?serviceKey=" + serviceKey +
               "&MobileOS=WEB" +
               "&MobileApp=Koreplan" +
               "&_type=json" +
               "&contentId=" + contentId +
               "&contentTypeId=" + contentTypeId +
               "&numOfRows=10" +
               "&pageNo=1";
    }
    
    /**
     * contentTypeId에 따른 응답 파싱
     */
    private Object parseResponse(String responseBody, String contentTypeId) throws Exception {
        switch (contentTypeId) {
            case "12": // 관광지
                return objectMapper.readValue(responseBody, 
                    objectMapper.getTypeFactory().constructParametricType(
                        ApiResponseDto.class, TourismInfoDto.class));
                
            case "14": // 문화시설
                return objectMapper.readValue(responseBody, 
                    objectMapper.getTypeFactory().constructParametricType(
                        ApiResponseDto.class, CultureInfoDto.class));
                
            case "28": // 레포츠
                return objectMapper.readValue(responseBody, 
                    objectMapper.getTypeFactory().constructParametricType(
                        ApiResponseDto.class, LeportsInfoDto.class));
                
            case "32": // 숙박
                return objectMapper.readValue(responseBody, 
                    objectMapper.getTypeFactory().constructParametricType(
                        ApiResponseDto.class, AccommodationInfoDto.class));
                
            case "38": // 쇼핑
                return objectMapper.readValue(responseBody, 
                    objectMapper.getTypeFactory().constructParametricType(
                        ApiResponseDto.class, ShoppingInfoDto.class));
                
            case "39": // 음식점
                return objectMapper.readValue(responseBody, 
                    objectMapper.getTypeFactory().constructParametricType(
                        ApiResponseDto.class, FoodInfoDto.class));
                
            default:
                log.warn("지원하지 않는 contentTypeId: {}", contentTypeId);
                throw new IllegalArgumentException("지원하지 않는 contentTypeId: " + contentTypeId);
        }
    }
    
    /**
     * 특정 타입으로 반환하는 편의 메서드들
     */
    public ResponseEntity<ApiResponseDto<TourismInfoDto>> getTourismInfo(String contentId) throws Exception {
        ResponseEntity<Object> response = getDetailIntro(contentId, "12");
        return ResponseEntity.ok((ApiResponseDto<TourismInfoDto>) response.getBody());
    }
    
    public ResponseEntity<ApiResponseDto<CultureInfoDto>> getCultureInfo(String contentId) throws Exception {
        ResponseEntity<Object> response = getDetailIntro(contentId, "14");
        return ResponseEntity.ok((ApiResponseDto<CultureInfoDto>) response.getBody());
    }
    
    public ResponseEntity<ApiResponseDto<LeportsInfoDto>> getLeportsInfo(String contentId) throws Exception {
        ResponseEntity<Object> response = getDetailIntro(contentId, "28");
        return ResponseEntity.ok((ApiResponseDto<LeportsInfoDto>) response.getBody());
    }
    
    public ResponseEntity<ApiResponseDto<AccommodationInfoDto>> getAccommodationInfo(String contentId) throws Exception {
        ResponseEntity<Object> response = getDetailIntro(contentId, "32");
        return ResponseEntity.ok((ApiResponseDto<AccommodationInfoDto>) response.getBody());
    }
    
    public ResponseEntity<ApiResponseDto<ShoppingInfoDto>> getShoppingInfo(String contentId) throws Exception {
        ResponseEntity<Object> response = getDetailIntro(contentId, "38");
        return ResponseEntity.ok((ApiResponseDto<ShoppingInfoDto>) response.getBody());
    }
    
    public ResponseEntity<ApiResponseDto<FoodInfoDto>> getFoodInfo(String contentId) throws Exception {
        ResponseEntity<Object> response = getDetailIntro(contentId, "39");
        return ResponseEntity.ok((ApiResponseDto<FoodInfoDto>) response.getBody());
    }
    
    /**
     * 응답에서 첫 번째 아이템 추출 (편의 메서드)
     */
    public <T> T getFirstItem(ApiResponseDto<T> response) {
        if (response != null && 
            response.getResponse() != null && 
            response.getResponse().getBody() != null && 
            response.getResponse().getBody().getItems() != null && 
            response.getResponse().getBody().getItems().getItem() != null && 
            !response.getResponse().getBody().getItems().getItem().isEmpty()) {
            return response.getResponse().getBody().getItems().getItem().get(0);
        }
        return null;
    }
}