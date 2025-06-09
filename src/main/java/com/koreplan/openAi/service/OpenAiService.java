package com.koreplan.openAi.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

import jakarta.annotation.PostConstruct;

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
                당신은 여행 플래너입니다.
                사용자에게 맞는 여행 일정을 추천해 주세요.

                지역: %s
                여행일 수: %d일
                동행 유형: %s
                선호 장소 타입: %s     
            	조건:
	            - Day 1 & Day %d는 1~2개의 장소 추천, 나머지 날은 3~4개의 장소만 추천해주세요.
	            - 하루 일정의 동선은 서로 가까운 장소 위주로 구성해주세요.
	            - 장소 간 이동 거리가 너무 멀지 않도록 해주세요.
	            - 효율적인 이동 거리를 구축해주세요.
	            - 간단한 설명을 포함해주세요.
	            - 반드시 Day 1부터 Day %d까지 모든 날짜 일정을 포함해주세요.

                아래와 같은 JSON 배열 형식으로 응답하세요:
                [
                  {
                    "day": 1,
                    "order": 1,
                    "region": "서울특별시",
                    "ward": "강남구",
                    "name": "경복궁",
                    "lat": 37.579617,
                    "lng": 126.977041,
                    "description": "조선시대의 대표 궁궐로 역사적 의미가 깊은 장소입니다."
                  },
                  ...
                ]
                """.formatted(region, days, companion, preferences, days, days);

            int estimatedInputTokens = gptPrompt.length() / 4;
            int estimatedOutputTokens = 200;

            if (!usageTracker.canProceed(estimatedInputTokens, estimatedOutputTokens)) {
                return "월 사용 예산을 초과하였습니다. (예산: $5)";
            }

            // 3. 안전하게 JSON 생성
            ObjectNode requestNode = mapper.createObjectNode();
            requestNode.put("model", "gpt-3.5-turbo");

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
	
	
	// 필터링 로직
	public List<JsonNode> filterExistingPlaces (JsonNode gptArray) {
		List<JsonNode> result = new ArrayList<>();
		
		for (JsonNode place : gptArray) {
			String regionName = normalizeRegionName(place.get("region").asText()); // 서울 특별시
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

	        // 3. 장소가 해당 ward, region 조합으로 실제 DB에 존재하는지 확인
	        boolean exists = dataRepository.existsByRegionCodeEntityAndWardCodeEntityAndTitle(regionEntity, wardEntity, placeName);
	        if (exists) {
	        	// day, order 포함하여 그대로 복사
	            ObjectNode node = mapper.createObjectNode();
	            node.put("day", place.get("day").asInt());
	            node.put("order", place.get("order").asInt());
	            node.put("region", regionName);
	            node.put("ward", wardName);
	            node.put("name", placeName);
	            node.put("description", place.get("description").asText());
	            node.put("lat", place.get("lat").asDouble());
	            node.put("lng", place.get("lng").asDouble());
	            result.add(node);
	        }
		}
		
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
    public List<JsonNode> fillWithDbPlaces(List<JsonNode> filteredPlaces, int desiredCount, String region, String ward) {
        
    	if (filteredPlaces.size() >= desiredCount) {
            return filteredPlaces;
        }

        // 1. region, ward 명으로 RegionCodeEntity, WardCodeEntity 조회 ---> 지역 정보 조회
        Optional<RegionCodeEntity> regionOpt = regionCodeRepository.findRegionByNameForAI(region);
        if (regionOpt.isEmpty()) return filteredPlaces;
        RegionCodeEntity regionEntity = regionOpt.get();

        Optional<WardCodeEntity> wardOpt = wardCodeRepository.findWardByNameAndRegionForAI(ward, regionEntity);
        if (wardOpt.isEmpty()) return filteredPlaces;
        WardCodeEntity wardEntity = wardOpt.get();


        // 정규화된 기존 이름 목록
        Set<String> normalizedExistingNames = filteredPlaces.stream()
                .map(p -> normalize(p.get("name").asText()))
                .collect(Collectors.toSet());
        
        // DB에서 전체 후보 조회
        List<DataEntity> allDbPlaces = dataRepository.findByRegionCodeEntityAndWardCodeEntity(regionEntity, wardEntity);

        // 기존 이름과 중복되지 않는 후보만 추출
        List<DataEntity> dbCandidates = allDbPlaces.stream()
                .filter(d -> !normalizedExistingNames.contains(normalize(d.getTitle())))
                .limit(desiredCount - filteredPlaces.size())
                .collect(Collectors.toList());
        
        List<JsonNode> filledList = new ArrayList<>(filteredPlaces);
        
        // 4. 마지막 day/order 구하기
        int currentDay = 1;
        int currentOrder = 0;
        if (!filteredPlaces.isEmpty()) {
            JsonNode last = filteredPlaces.get(filteredPlaces.size() - 1);
            currentDay = last.get("day").asInt();
            currentOrder = last.get("order").asInt();
        }

        // 5. 보완장소 추가
        for (DataEntity data : dbCandidates) {
            ObjectNode node = mapper.createObjectNode();
            String name = data.getTitle();
            String normalizedName = normalize(name);
            
            node.put("name", name);
            node.put("region", region);
            node.put("ward", ward);
            node.put("lat", data.getMapx());
            node.put("lng", data.getMapy());
            
            // 순차적으로 day/order 증가
            if (currentOrder >= 3) {
                currentDay++;
                currentOrder = 1;
            } else {
                currentOrder++;
            }

            node.put("day", currentDay);
            node.put("order", currentOrder);
            // ✅ description은 DB에 없으므로 기본값만 사용
            node.put("description", "설명 준비 중입니다.");

            filledList.add(node);
        }

        return filledList;
    }
    
    // 공백 제거 + 소문자 변환
    private String normalize(String input) {
        return input == null ? "" : input.toLowerCase().replaceAll("\\s+", "");
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
    
    public List<JsonNode> getFilteredAndFilledPlaces(JsonNode gptArray, int gptCount) {
        // 1. 필터링
        List<JsonNode> filtered = filterExistingPlaces(gptArray);

        // 지역별, 동별로 그룹핑해서 부족한 개수 채우기
        // (예: 여러 날, 여러 동이 섞여 있어도 동별로 보완 처리)
        // 간단히 한 번에 처리하려면 모든 장소 같은 지역, 동 이름 기준으로 보완
        
        if (filtered.isEmpty()) {
        	// 만약 필터링 결과가 없으면, gptArray 첫 번째 장소 기준으로 보완
            if (gptArray.size() > 0) {
                JsonNode first = gptArray.get(0);
                String region = normalizeRegionName(first.get("region").asText());
                String ward = first.get("ward").asText();
                return fillWithDbPlaces(filtered, gptCount, region, ward);
            } else {
                return filtered;
            }
        }

        // 2. region, ward 정보 얻기 (첫 장소 기준)
        // 여러 동/지역 혼합일 경우 간단히 전체 지역/동 기준으로 보완할 수도 있음
        // 여기서는 첫 장소의 지역, 동 기준으로 채움
        String region = filtered.get(0).get("region").asText();
        String ward = filtered.get(0).get("ward").asText();

        return fillWithDbPlaces(filtered, gptCount, region, ward);
    }
	
}
