package com.koreplan.service.festival;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koreplan.dto.ApiResponseDto;
import com.koreplan.dto.festival.FestivalDetailInfoDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class DetailFestivalService {
	@Value("${publicDataKey}")
    private String serviceKey;
	private final String API_BASE_URL = "https://apis.data.go.kr/B551011/KorService2/detailInfo2";
    private final ObjectMapper objectMapper;
    
    public ResponseEntity<Object> getDetailIntro(String contentId) throws Exception {
    	String fullUrl = API_BASE_URL 
                + "?serviceKey=" + serviceKey
                + "&MobileOS=WEB"
                + "&MobileApp=Koreplan"
                + "&_type=json"
                + "&contentId=" + contentId
    			+ "&contentTypeId=15";
                
		URL url = new URL(fullUrl);

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Content-type", "application/json");
		conn.setConnectTimeout(1000000000);
		conn.setReadTimeout(1000000000);

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
		try {
		    
			Object detailFestival = objectMapper.readValue(responseBody, 
                    objectMapper.getTypeFactory().constructParametricType(
                        ApiResponseDto.class, FestivalDetailInfoDto.class));
		    return ResponseEntity.ok(detailFestival); 
		} catch (Exception e) {
		    System.err.println("JSON 변환 중 오류 발생: " + responseBody);
		    e.printStackTrace();
		    throw new RuntimeException("JSON 변환 중 오류 발생: " + e.getMessage());
		}
	
    }
}
