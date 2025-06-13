package com.koreplan.service.image;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koreplan.dto.image.ImageApiResponseDto;
import com.koreplan.repository.festival.FestivalRepository;
import com.koreplan.data.repository.DataRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImagesLoadService {
	
	// 기본 ObjectMapper는 사용하지 않고, 이미지 API 전용 ObjectMapper 생성
	@Autowired
	private DataRepository dataRepository;
	@Autowired
	private FestivalRepository festivalRepository;
    @Value("${publicDataKey}")
    private String key;
    
    private final String apiUrl = "https://apis.data.go.kr/B551011/KorService2/detailImage2";
    
    // contentId 유효성 검사 패턴 (숫자만 허용)
    private static final Pattern CONTENT_ID_PATTERN = Pattern.compile("^\\d+$");
    
    // 이미지 API 전용 ObjectMapper (빈 문자열 처리 설정)
    private final ObjectMapper imageApiObjectMapper = new ObjectMapper()
            .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    
    public ImageApiResponseDto getImages(String contentId) throws Exception {
        
        // 1. contentId 기본 유효성 검사
        if (!isValidContentId(contentId)) {
            log.warn("유효하지 않은 contentId 형식: {}", contentId);
            throw new IllegalArgumentException("유효하지 않은 contentId 형식입니다");
        }
        
        // 2. DB에서 여행지 존재 여부 확인 (선택사항)
        if (!isSpotExistsInDB(contentId)) {
            log.warn("DB에 존재하지 않는 여행지: {}", contentId);
            throw new IllegalArgumentException("존재하지 않는 여행지입니다");
        }
        
        // 3. API 호출
        String fullUrl = apiUrl 
                + "?serviceKey=" + key
                + "&MobileOS=WEB"
                + "&MobileApp=Koreplan"
                + "&_type=json"
                + "&contentId=" + contentId;
        
        log.info("이미지 API 호출: contentId={}", contentId);
        
        URL url = new URL(fullUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        
        BufferedReader rd;
        if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            log.error("API 호출 실패: HTTP {}, {}", conn.getResponseCode(), conn.getResponseMessage());
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
        log.debug("API 응답: {}", responseBody);
        
        try {
            // 4. 이미지 API 전용 ObjectMapper 사용 (빈 문자열 처리 포함)
            ImageApiResponseDto imageResponse = imageApiObjectMapper.readValue(responseBody, ImageApiResponseDto.class);
            
            // 5. API 결과 검증
            if (imageResponse.getResponse() == null || 
                imageResponse.getResponse().getHeader() == null) {
                throw new RuntimeException("API 응답 형식이 올바르지 않습니다");
            }
            
            String resultCode = imageResponse.getResponse().getHeader().getResultCode();
            if (!"0000".equals(resultCode)) {
                String resultMsg = imageResponse.getResponse().getHeader().getResultMsg();
                log.warn("API 오류 응답: code={}, msg={}", resultCode, resultMsg);
                throw new RuntimeException("API 오류: " + resultMsg);
            }
            
            // 6. 이미지 개수 로깅
            int totalCount = imageResponse.getResponse().getBody() != null ? 
                           imageResponse.getResponse().getBody().getTotalCount() : 0;
            log.info("이미지 조회 완료: contentId={}, 총 {}개", contentId, totalCount);
            
            return imageResponse;
            
        } catch (Exception e) {
            log.error("JSON 변환 중 오류 발생: {}", responseBody, e);
            throw new RuntimeException("JSON 변환 중 오류 발생: " + e.getMessage());
        }
    }
    
    /**
     * contentId 형식 유효성 검사
     * - 숫자로만 구성되어야 함
     * - 길이 제한 (1-15자리)
     */
    private boolean isValidContentId(String contentId) {
        if (contentId == null || contentId.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = contentId.trim();
        
        // 숫자만 허용, 길이 1-15자리
        return CONTENT_ID_PATTERN.matcher(trimmed).matches() && 
               trimmed.length() >= 1 && trimmed.length() <= 15;
    }
    
    /**
     * DB에서 여행지 존재 여부 확인
     * (실제 DB 구조에 맞게 수정 필요)
     */
    private boolean isSpotExistsInDB(String contentId) {
        try {
            // 여행지 테이블에서 확인
            if (dataRepository.existsByContentId(contentId)) {
                return true;
            }
            
            
			// 축제 테이블에서 확인
            if (festivalRepository.existsByContentId(contentId)) {
                return true;
            }
            
            return false;
        } catch (Exception e) {
            log.warn("DB 조회 중 오류: {}", e.getMessage());
            // DB 조회 실패 시에는 API 호출을 허용
            return false;
        }
    }
}