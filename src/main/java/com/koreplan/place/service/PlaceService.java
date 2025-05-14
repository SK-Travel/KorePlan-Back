package com.koreplan.place.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koreplan.place.dto.PlaceDTO;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class PlaceService {

    @Value("${naver.client.id}")
    private String clientId;

    @Value("${naver.client.secret}")
    private String clientSecret;

    // ✅ 실행 시 인증 정보 출력 (디버깅용)
    @PostConstruct
    public void checkKeys() {
        System.out.println("✅ Naver Client ID: " + clientId);
        System.out.println("✅ Naver Client Secret: " + clientSecret);
    }

    public PlaceDTO getPlaceByKeyword(String keyword) {
        try {
            String encoded = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String url = "https://naveropenapi.apigw.ntruss.com/map-geocode/v2/geocode?query=" + encoded;

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-NCP-APIGW-API-KEY-ID", clientId);   // ✅ 공식 명칭
            headers.set("X-NCP-APIGW-API-KEY", clientSecret);  // ✅ 공식 명칭
            
            HttpEntity<String> entity = new HttpEntity<>(headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            System.out.println("🟡 Naver 응답: " + response.getBody());
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode addr = root.path("addresses");

            if (!addr.isArray() || addr.size() == 0) {
                return null;
            }

            JsonNode firstAddr = addr.get(0);
            return new PlaceDTO(
                    keyword,
                    firstAddr.path("roadAddress").asText(),
                    firstAddr.path("y").asDouble(),
                    firstAddr.path("x").asDouble()
            );

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
