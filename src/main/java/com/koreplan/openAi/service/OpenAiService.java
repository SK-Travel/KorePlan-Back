package com.koreplan.openAi.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.koreplan.area.entity.RegionCodeEntity;
import com.koreplan.area.entity.WardCodeEntity;
import com.koreplan.area.repository.RegionCodeRepository;
import com.koreplan.area.repository.WardCodeRepository;
import com.koreplan.data.entity.DataEntity;
import com.koreplan.data.repository.DataRepository;
import com.koreplan.openAi.UsageTracker;

import io.netty.channel.ChannelOption;
import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Service
public class OpenAiService {
	
	// API키 읽기
	@Value("${openai.api.key}")
	private String openaiApiKey;
	
	private WebClient webClient;
	
	@Autowired
	private UsageTracker usageTracker;
	
	@Autowired
	private DataRepository dataRepository;
	
	@Autowired
	private RegionCodeRepository regionCodeRepository;

	@Autowired
	private WardCodeRepository wardCodeRepository;
	
    // ObjectMapper 객체 필드로 선언해서 재사용
    private final ObjectMapper mapper = new ObjectMapper();
    
	
	 // 빈 생성 후 초기화 메서드 (WebClient 설정)
	@PostConstruct
	public void init() {
		// WebClient 빌더를 통해 OpenAI API 기본 URL과 헤더 설정
		// Authorization 헤더에 Bearer + API 키를 넣음.
		webClient = WebClient.builder()
				.baseUrl("https://api.openai.com/v1") // OPEN API 기본 URL
				.defaultHeader("Authorization", "Bearer " + openaiApiKey)  // 인증용 헤더
				.defaultHeader("Content-Type", "application/json") // JSON 형식 명시
		        .clientConnector(
	                new ReactorClientHttpConnector(
	                    HttpClient.create()
	                        .responseTimeout(Duration.ofSeconds(30)) // 최대 30초 응답 제한
	                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
	                )
	            )
				.build();
	}
	
	
	/**
	 * OpenAI Chat Completion API 호출 메서드
     * @param userMessage 사용자가 입력한 질문
     * @return OpenAI API에서 받은 JSON 응답 문자열
	 */
	public String chatWithGpt(String userMessage) {
	    try {
            // 1. JSON 파싱
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(userMessage);

            String region = node.get("region").asText();
            int days = node.get("days").asInt();
            String companion = node.get("companion").asText();
            String preferences = node.get("preferences").asText();

            // 2. GPT용 프롬프트 구성
            String gptPrompt = """
            		당신은 여행 일정 플래너입니다. 다음 조건에 맞는 일정(JSON 배열)만 생성하세요: 지역: %s, 여행일 수: %d일, 동행 유형: %s, 선호 장소: %s
            		일정 조건: Day 1과 Day %d는 1~2개 장소만 추천, 중간 날짜들은 각 3~4개 장소 추천, 장소 간 이동 거리는 짧게 구성
            		응답 형식: JSON 배열만 출력하세요. 코드블록(```json 등)은 절대 포함하지 마세요.예시: [{"day": 1, "order": 1, "region": "서울특별시", "ward": "강남구", "name": "경복궁", "lat": 37.579617, "lng": 126.977041}, ...]
            		""".formatted(region, days, companion, preferences, days);

            int estimatedInputTokens = gptPrompt.length() / 4;
            int estimatedOutputTokens = 200;

            if (!usageTracker.canProceed(estimatedInputTokens, estimatedOutputTokens)) {
                return "월 사용 예산을 초과하였습니다. (예산: $5)";
            }

            // 3. 안전하게 JSON 생성
            ObjectNode requestNode = mapper.createObjectNode();
            requestNode.put("model", "gpt-4o");
            requestNode.put("max_tokens", 1000); // 토큰 제한 추가
//            requestNode.put("temperature", 0.7); // 0.0 ~ 2.0 지피티의 창의성 디폴트는 0.7임

            ArrayNode messages = mapper.createArrayNode();
            ObjectNode userNode = mapper.createObjectNode();
            userNode.put("role", "user");
            userNode.put("content", gptPrompt);
            messages.add(userNode);

            requestNode.set("messages", messages);

            String requestBody = mapper.writeValueAsString(requestNode); // 최종 JSON 문자열

            // 디버깅용 로그
            System.out.println("GPT 요청 바디: " + requestBody);

            // 4. WebClient로 OpenAI API 호출
            String rawJson = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

//            System.out.println("최초 대답:" + rawJson);
            // 5. 응답에서 message.content 추출
            JsonNode root = mapper.readTree(rawJson);
            
            return root.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            return "OPEN AI 응답 처리 중 오류 내용: " + e.getMessage();
        }
	}	
	// 지피티에서 보내온 거 보정하기
	private String normalizeRegionName(String region) {
        return switch (region) {
        case "서울" -> "서울특별시";
        case "부산" -> "부산광역시";
        case "대구" -> "대구광역시";
        case "인천" -> "인천광역시";
        case "광주" -> "광주광역시";
        case "대전" -> "대전광역시";
        case "울산" -> "울산광역시";
        case "세종" -> "세종특별자치시";
        case "경기" -> "경기도";
        case "강원" -> "강원특별자치도";
        case "충북" -> "충청북도";
        case "충남" -> "충청남도";
        case "전북" -> "전라북도";
        case "전남" -> "전라남도";
        case "경북" -> "경상북도";
        case "경남" -> "경상남도";
        case "제주" -> "제주특별자치도";
        default -> region;
	    };
	}
	
	// 대소문자 무시, 공백 무시(같게 한단 뜻)
	private boolean isSimilarName(String name1, String name2) {
	    if (name1 == null || name2 == null) return false;
	    String norm1 = name1.replaceAll("\\s+", "").toLowerCase();
	    String norm2 = name2.replaceAll("\\s+", "").toLowerCase();
	    return norm1.equals(norm2);
	}
	
	// 필터링 로직
	public List<JsonNode> filterExistingPlaces (JsonNode gptArray, List<Integer> themeIds) {
		List<JsonNode> result = new ArrayList<>();
		
		for (JsonNode place : gptArray) {
			String regionName = normalizeRegionName(place.get("region").asText()); // 서울 특별시
//			String regionName = place.get("region").asText();
	        String wardName = place.has("ward") ? place.get("ward").asText() : null;  // ex) 강남구
	        String placeName = place.has("name") ? place.get("name").asText() : null;
	        
	        if (wardName == null || placeName == null) continue;

	        // 1. regioncode 조회
	        Optional<RegionCodeEntity> regionOpt = regionCodeRepository.findRegionByNameForAI(regionName);
	        if (regionOpt.isEmpty()) continue;
	        RegionCodeEntity regionEntity = regionOpt.get();
	      
	        // 2. ward 이름과 region 조합으로 WardCodeEntity 조회
	        Optional<WardCodeEntity> wardOpt = wardCodeRepository.findWardByNameAndRegionForAI(wardName, regionEntity);
	        if (wardOpt.isEmpty()) continue;
	        WardCodeEntity wardEntity = wardOpt.get();

	        // ✅ 유사한 이름 비교 (공백, 대소문자 무시)
	        List<DataEntity> candidates = dataRepository.findByRegionCodeEntityAndWardCodeEntityAndThemeIn(regionEntity, wardEntity, themeIds);
	        for (DataEntity d : candidates) { // 반복문으로 변경
	            if (isSimilarName(d.getTitle(), placeName)) {
	                // day, order 포함하여 그대로 복사
	                ObjectNode node = mapper.createObjectNode();
	                node.put("day", place.get("day").asInt());
	                node.put("order", place.get("order").asInt());
	                node.put("region", regionName);
	                node.put("ward", wardName);
	                node.put("name", placeName);
	                node.put("lat", place.get("lat").asDouble());
	                node.put("lng", place.get("lng").asDouble());
//	            node.put("description", place.get("description").asText());
	                node.put("contentId", d.getContentId()); // 🔧 contentId 추가
	                node.put("firstimage", d.getFirstimage()); // 이미지 추가
	                result.add(node);
	                break; // 🔧 매칭되었으므로 반복 종료
	            }
	        }
		}
		System.out.println("[필터링] GPT 추천 " + gptArray.size() + " → DB 존재 " + result.size());
		return result;
	}
	
	
	
	/**
     * filteredPlaces 리스트가 desiredCount만큼 부족할 경우,
     * DB에서 region/ward에 해당하는 장소를 보완해서 추가한다.
     * 
     * @param filteredPlaces GPT 추천 후 DB 필터링된 장소 리스트 (JsonNode 리스트)
     * @param desiredCount 최소 추천 장소 개수
     * @param region 지역명 (예: "서울특별시")
     * @param ward 동명 (예: "강남구")
     * @return 보완된 장소 리스트 (filteredPlaces에 부족분 추가됨)
     */
    public List<JsonNode> fillWithDbPlacesOnly(List<String> missingKeys, String region, String ward, List<Integer> themeIds) {
        
    	 RegionCodeEntity regionEntity  = regionCodeRepository.findRegionByNameForAI(region).orElse(null);
    	    if (regionEntity == null) return List.of();

    	    WardCodeEntity wardEntity = wardCodeRepository.findWardByNameAndRegionForAI(ward, regionEntity).orElse(null);
    	    if (wardEntity == null) return List.of();

    	    List<DataEntity> allDbPlaces = dataRepository.findByRegionCodeEntityAndWardCodeEntityAndThemeIn(regionEntity, wardEntity, themeIds);

    	    // 중복 방지용
    	    Set<String> usedNames = new HashSet<>();
    	    List<JsonNode> result = new ArrayList<>();

    	    int index = 0;
    	    for (String key : missingKeys) {
    	        if (index >= allDbPlaces.size()) break;

    	        while (index < allDbPlaces.size()) {
    	            DataEntity data = allDbPlaces.get(index++);
    	            String norm = normalize(data.getTitle());
    	            if (usedNames.contains(norm)) continue;
    	            usedNames.add(norm);

    	            ObjectNode node = mapper.createObjectNode();
    	            node.put("region", region);
    	            node.put("ward", ward);
    	            node.put("name", data.getTitle());
//    	            node.put("description", "설명 준비 중입니다.");
    	            node.put("lng", Double.parseDouble(data.getMapx()));
    	            node.put("lat", Double.parseDouble(data.getMapy()));
    	            node.put("contentId", data.getContentId());
    	            node.put("firstimage", data.getFirstimage());
    	            result.add(node);
    	            break;
    	        }
    	    }
        return result;
    }
    
    // 공백 제거 + 소문자 변환
    private String normalize(String input) {
        return input == null ? "" : input.toLowerCase().replaceAll("[\\s()\\[\\]\\p{Punct}]", "");
    }
    
    /**
     * GPT가 준 장소 JSON 배열을
     * - DB 존재 여부로 필터링하고
     * - 부족한 개수는 DB에서 채워서 보완함
     * 
     * @param gptArray GPT가 추천한 장소 JSON 배열
     * @param gptCount GPT 추천 장소 수
     * @return 필터링 + 보완된 장소 리스트
     */
    
    public List<JsonNode> getFilteredAndFilledPlaces(JsonNode gptArray, int gptCount, List<Integer> themeIds) {
        List<JsonNode> filtered = filterExistingPlaces(gptArray, themeIds);


        // 1. 원본 GPT에서 day-order -> place 매핑
        Map<String, JsonNode> gptDayOrderMap = new HashMap<>();
        
        for (JsonNode place : gptArray) {
            String key = place.get("day").asInt() + "-" + place.get("order").asInt();
            gptDayOrderMap.put(key, place);
        }

        // 2. 필터링된 결과에서 있는 day-order 확인
        Set<String> existingKeys = filtered.stream()
                .map(p -> p.get("day").asInt() + "-" + p.get("order").asInt())
                .collect(Collectors.toSet());

        // 3. 누락된 자리 목록 수집
        List<String> missingKeys = new ArrayList<>();
        List<JsonNode> missingGptPlaces = new ArrayList<>();
        for (Map.Entry<String, JsonNode> entry : gptDayOrderMap.entrySet()) {
            if (!existingKeys.contains(entry.getKey())) {
                missingKeys.add(entry.getKey());
                missingGptPlaces.add(entry.getValue());
            }
        }
        
        
        // region/ward 기반으로 누락된 위치 보완
        List<JsonNode> dbFilled = new ArrayList<>();
        
        if (!missingGptPlaces.isEmpty()) {
            String region = normalizeRegionName(missingGptPlaces.get(0).get("region").asText());
            String ward = missingGptPlaces.get(0).get("ward").asText();

            // ✅ 누락된 자리만큼 DB에서 보완
            dbFilled = fillWithDbPlacesOnly(
                missingKeys,              // 💡 누락된 자리만
                region,
                ward,
                themeIds
            );

            // 💡 보완된 DB 장소를 누락된 GPT 자리의 day/order에 정확히 맞춰줌
            for (int i = 0; i < dbFilled.size() && i < missingGptPlaces.size(); i++) {
                ObjectNode filled = (ObjectNode) dbFilled.get(i);
                JsonNode original = missingGptPlaces.get(i);
                filled.put("day", original.get("day").asInt());
                filled.put("order", original.get("order").asInt());
            }
        }
        
        
        // ✅ 최종 합치기 (기존 유지 + 보완)
        List<JsonNode> finalList = new ArrayList<>(filtered);
        finalList.addAll(dbFilled);

        // 중복 제거 (같은 day-order는 하나만)
        Map<String, JsonNode> uniqueMap = new LinkedHashMap<>();
        for (JsonNode place : finalList) {
            String key = place.get("day").asInt() + "-" + place.get("order").asInt();
            uniqueMap.put(key, place);
        }

        List<JsonNode> dedupedList = new ArrayList<>(uniqueMap.values());
        dedupedList.sort(Comparator
        	    .comparingInt(n -> ((JsonNode) n).get("day").asInt())
        	    .thenComparingInt(n -> ((JsonNode) n).get("order").asInt()));
        
        return dedupedList;
    }
	
}
