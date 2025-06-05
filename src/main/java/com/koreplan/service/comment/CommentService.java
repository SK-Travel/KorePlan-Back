package com.koreplan.service.comment;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koreplan.dto.comment.CommentDto;
import com.koreplan.dto.comment.CommentResponseDto;
import com.koreplan.dto.image.ImageApiResponseDto;
import com.koreplan.service.image.ImagesLoadService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {
	private final ObjectMapper objectMapper;
	
    @Value("${publicDataKey}")
    private String key;
    
    private final String apiUrl = "https://apis.data.go.kr/B551011/KorService2/detailCommon2";
    
    public CommentResponseDto getInfomation(String contentId) throws Exception {
    	String fullUrl = apiUrl 
                + "?serviceKey=" + key
                + "&MobileOS=WEB"
                + "&MobileApp=Koreplan"
                + "&_type=json"
                + "&contentId=" + contentId;
                
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
		log.info(responseBody);
		try {
		    // 문자열을 ImageApiResponseDto 객체로 변환
		    CommentResponseDto commentResponse = objectMapper.readValue(responseBody, CommentResponseDto.class);
		    return commentResponse;  // ResponseEntity가 아닌 ImageApiResponseDto 직접 반환
		} catch (Exception e) {
		    System.err.println("JSON 변환 중 오류 발생: " + responseBody);
		    e.printStackTrace();
		    throw new RuntimeException("JSON 변환 중 오류 발생: " + e.getMessage());
		}
    }
}
