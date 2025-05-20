package com.koreplan.place.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koreplan.place.dto.PlaceDTO;

import jakarta.annotation.PostConstruct;

@Service
public class PlaceService {

    @Value("${naver.client.id}")
    private String clientId;

    @Value("${naver.client.secret}")
    private String clientSecret;

    @Value("${google.places.api.key}")
    private String apiKey;

    // ✅ 실행 시 인증 정보 출력 (디버깅용)
    @PostConstruct
    public void checkKeys() {
        System.out.println("✅ Naver Client ID: " + clientId);
        System.out.println("✅ Naver Client Secret: " + clientSecret);
        System.out.println("✅ Google Places API Key: " + apiKey);
    }

    // 검색 API
    public List<PlaceDTO> getPlacesByKeyword(String keyword) {
        List<PlaceDTO> resultList = new ArrayList<>();

        try {
            String text = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String apiUrl = "https://openapi.naver.com/v1/search/local.json?query=" + text + "&display=5&start=1";

            URL url = new URL(apiUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("X-Naver-Client-Id", clientId);
            con.setRequestProperty("X-Naver-Client-Secret", clientSecret);
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = con.getResponseCode();
            InputStream responseStream = (responseCode == 200) ? con.getInputStream() : con.getErrorStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(responseStream, StandardCharsets.UTF_8));
            StringBuilder responseBuilder = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                responseBuilder.append(line);
            }

            String responseJson = responseBuilder.toString();
            System.out.println("🟡 Naver 응답: " + responseJson);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseJson);
            JsonNode items = root.path("items");

            if (items.isArray()) {
                for (JsonNode item : items) {
                    PlaceDTO dto = new PlaceDTO(
                            item.path("title").asText().replaceAll("<[^>]*>", ""),
                            item.path("link").asText(),
                            item.path("category").asText(),
                            item.path("address").asText(),
                            item.path("roadAddress").asText(),
                            item.path("telephone").asText(),
                            item.path("mapx").asText(),
                            item.path("mapy").asText()
                    );
                    resultList.add(dto);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultList;
    }
}
