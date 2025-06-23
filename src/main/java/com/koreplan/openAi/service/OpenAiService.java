package com.koreplan.openAi.service;

import java.text.Normalizer;
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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import reactor.netty.http.client.HttpClient;

@Service
@Transactional
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

            // 2. GPT용 프롬프트 구성 - 주소 정보 추가 요청
            String gptPrompt = """
            		당신은 여행 일정 플래너입니다. 다음 조건에 맞는 일정(JSON 배열)만 생성하세요: 지역: %s, 여행일 수: %d일, 동행 유형: %s, 선호 장소: %s
            		
            		⚠️ 중요: 
            		- 각 날짜별로 최소 3개 이상의 장소를 추천하세요!
            		- 각 장소마다 정확한 주소를 포함하세요
            		- 같은 주소의 장소는 중복 추천하지 마세요
            		- 1박 2일인 경우: Day 1에 3개, Day 2에 3개 (총 6개)
            		- 2박 3일인 경우: Day 1에 3개, Day 2에 4개, Day 3에 3개 (총 10개)
            		- 3박 이상인 경우: 첫날/마지막날 3개씩, 중간 날짜들은 4개씩
            		
            		장소 간 이동 거리는 짧게 구성하고, 같은 구(ward) 내의 장소들로 추천하세요.
            		응답 형식: JSON 배열만 출력하세요. 코드블록(```json 등)은 절대 포함하지 마세요.
            		예시: [{"day": 1, "order": 1, "region": "서울특별시", "ward": "종로구", "title": "경복궁", "address": "서울특별시 종로구 세종대로 175"}, {"day": 1, "order": 2, "region": "서울특별시", "ward": "종로구", "title": "북촌한옥마을", "address": "서울특별시 종로구 계동길 37"}, ...]
            		""".formatted(region, days, companion, preferences);

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
	
	// 주소 정규화 (구 + 주요 도로명 추출)
	private String normalizeAddress(String address) {
	    if (address == null) return "";
	    
	    // "서울특별시 종로구 세종대로 175" → "종로구 세종대로"
	    Pattern pattern = Pattern.compile("(\\w+구)\\s+(\\w+[로길동])");
	    Matcher matcher = pattern.matcher(address);
	    
	    if (matcher.find()) {
	        return matcher.group(1) + " " + matcher.group(2);
	    }
	    
	    // 패턴이 안 맞으면 원본 반환
	    return address.toLowerCase().replaceAll("\\s+", "");
	}
	
	// 주소 유사도 매칭 (DB의 addr1만 사용)
	private boolean isSimilarAddress(String gptAddress, String dbAddr1) {
	    if (gptAddress == null || dbAddr1 == null) return false;
	    
	    String norm1 = normalizeAddress(gptAddress);
	    String norm2 = normalizeAddress(dbAddr1);
	    
	    System.out.println("주소 비교: '" + gptAddress + "' -> '" + norm1 + "' vs '" + dbAddr1 + "' -> '" + norm2 + "'");
	    
	    boolean result = norm1.equals(norm2);
	    System.out.println("→ 주소 매칭 결과: " + result);
	    return result;
	}
	
	// 개선된 타이틀 매칭 (띄어쓰기 모두 제거)
	private boolean isSimilarTitle(String title1, String title2) {
	    if (title1 == null || title2 == null) return false;
	    
	    // 띄어쓰기, 특수문자 모두 제거 후 비교
	    String norm1 = title1.replaceAll("[\\s\\-\\(\\)\\[\\]\\p{Punct}]", "").toLowerCase();
	    String norm2 = title2.replaceAll("[\\s\\-\\(\\)\\[\\]\\p{Punct}]", "").toLowerCase();
	    
	    System.out.println("타이틀 비교: '" + title1 + "' -> '" + norm1 + "' vs '" + title2 + "' -> '" + norm2 + "'");
	    
	    // 완전 일치 또는 포함 관계
	    boolean result = norm1.equals(norm2) || norm1.contains(norm2) || norm2.contains(norm1);
	    System.out.println("→ 타이틀 매칭 결과: " + result);
	    return result;
	}
	
	// 통합 매칭 함수 (주소 우선, 타이틀 보조)
	private boolean isMatchingPlace(String gptTitle, String gptAddress, String dbTitle, String dbAddr1) {
	    // 1단계: 주소 검사 (DB의 addr1만 사용)
	    if (isSimilarAddress(gptAddress, dbAddr1)) {
	        System.out.println("→ 주소 매칭 성공!");
	        return true;
	    }
	    
	    // 2단계: 타이틀 검사 (띄어쓰기 제거 후)
	    if (isSimilarTitle(gptTitle, dbTitle)) {
	        System.out.println("→ 타이틀 매칭 성공!");
	        return true;
	    }
	    
	    System.out.println("→ 매칭 실패");
	    return false;
	}
	
	// GPT 응답에서 중복 주소 제거
	private List<JsonNode> removeDuplicateAddresses(JsonNode gptArray) {
	    Set<String> usedAddresses = new HashSet<>();
	    List<JsonNode> filteredPlaces = new ArrayList<>();
	    
	    for (JsonNode place : gptArray) {
	        String address = place.has("address") ? place.get("address").asText() : "";
	        String normalizedAddr = normalizeAddress(address);
	        
	        if (usedAddresses.add(normalizedAddr)) {
	            filteredPlaces.add(place);
	            System.out.println("추가된 장소: " + place.get("title").asText() + " (" + address + ")");
	        } else {
	            System.out.println("중복 주소로 제외: " + place.get("title").asText() + " (" + address + ")");
	        }
	    }
	    
	    return filteredPlaces;
	}
	
	// 필터링 로직 - 주소 기반 매칭 적용
	@Transactional(readOnly = true)
	public List<JsonNode> filterExistingPlaces(JsonNode gptArray, List<Integer> themeIds) {
		List<JsonNode> result = new ArrayList<>();
		
		// 먼저 중복 주소 제거
		List<JsonNode> uniquePlaces = removeDuplicateAddresses(gptArray);
		
		for (JsonNode place : uniquePlaces) {
			String regionName = normalizeRegionName(place.get("region").asText());
	        String wardName = place.has("ward") ? place.get("ward").asText() : null;
	        String placeName = place.has("title") ? place.get("title").asText() : null;
	        String placeAddress = place.has("address") ? place.get("address").asText() : null;
	        
	        if (wardName == null || placeName == null) continue;

	        // 1. regioncode 조회
	        Optional<RegionCodeEntity> regionOpt = regionCodeRepository.findRegionByNameForAI(regionName);
	        if (regionOpt.isEmpty()) continue;
	        RegionCodeEntity regionEntity = regionOpt.get();
	      
	        // 2. ward 이름과 region 조합으로 WardCodeEntity 조회
	        Optional<WardCodeEntity> wardOpt = wardCodeRepository.findWardByNameAndRegionForAI(wardName, regionEntity);
	        if (wardOpt.isEmpty()) continue;
	        WardCodeEntity wardEntity = wardOpt.get();

	        // 3. 주소 + 타이틀 기반 매칭 (DB의 addr1만 사용)
	        List<DataEntity> candidates = dataRepository.findByRegionCodeEntityAndWardCodeEntityAndThemeIn(regionEntity, wardEntity, themeIds);
	        for (DataEntity data : candidates) {
	        	System.out.println("[후보] " + data.getTitle());
	            if (isMatchingPlace(placeName, placeAddress, data.getTitle(), data.getAddr1())) {
	            	if (data.getC1Code().equals("AC")) {
	            		continue;
	            	}
	            	ObjectNode node = mapper.createObjectNode();
	            	node.put("day", place.get("day").asInt());
	            	node.put("order", place.get("order").asInt());
	            	node.put("id", data.getId());
	            	node.put("title", data.getTitle());
	            	node.put("mapx", data.getMapx());
	            	node.put("mapy", data.getMapy());
	            	node.put("contentId", data.getContentId());
	            	node.put("firstimage", data.getFirstimage());
	            	node.put("firstimage2", data.getFirstimage2());
	            	node.put("addr1", data.getAddr1());
	            	node.put("addr2", data.getAddr2());
	            	node.put("c1Code", data.getC1Code());
	            	node.put("c2Code", data.getC2Code());
	            	node.put("c3Code", data.getC3Code());
	            	node.put("tel", data.getTel());
	            	node.put("contentTypeId", data.getTheme());
	            	node.put("theme", data.getTheme());
    	            node.put("regionName", data.getRegionCodeEntity().getName());
    	            node.put("regionCode", data.getRegionCodeEntity().getRegioncode());
    	            node.put("wardName", data.getWardCodeEntity().getName());
    	            node.put("wardCode", data.getWardCodeEntity().getWardcode());
    	            node.put("viewCount", data.getViewCount());
    	            node.put("rating", data.getRating());
    	            node.put("reviewCount", data.getReviewCount());
    	            node.put("likeCount", data.getLikeCount());
    	            
    	            System.out.println("[매칭 성공] GPT: " + placeName + " ↔ DB: " + data.getTitle());
    	            result.add(node);
	                break; // 매칭되었으므로 반복 종료
	            }
	        }
		}
		return result;
	}
	
	/**
     * filteredPlaces 리스트가 desiredCount만큼 부족할 경우,
     * DB에서 region/ward에 해당하는 장소를 보완해서 추가한다.
     */
	@Transactional(readOnly = true)
    public List<JsonNode> fillWithDbPlacesOnly(List<String> missingKeys, String region, String ward, List<Integer> themeIds) {
        
        RegionCodeEntity regionEntity = regionCodeRepository.findRegionByNameForAI(region).orElse(null);
        if (regionEntity == null) {
            System.out.println("지역을 찾을 수 없음: " + region);
            return List.of();
        }

        WardCodeEntity wardEntity = wardCodeRepository.findWardByNameAndRegionForAI(ward, regionEntity).orElse(null);
        if (wardEntity == null) {
            System.out.println("구를 찾을 수 없음: " + ward);
            return List.of();
        }

        // 필요한 개수만큼 score 높은 순으로 조회
        int needed = missingKeys.size();
        Pageable pageable = PageRequest.of(0, needed, Sort.by(Sort.Direction.DESC, "score"));
        List<DataEntity> topRatedPlaces = dataRepository.findByRegionCodeEntityAndWardCodeEntityAndThemeIn(
            regionEntity, wardEntity, themeIds, pageable
        ).getContent();
        
        System.out.println("DB에서 조회된 상위 " + needed + "개 장소 (score 높은 순): " + topRatedPlaces.size());

        // 중복 방지용
        Set<String> usedNames = new HashSet<>();
        List<JsonNode> result = new ArrayList<>();

        // 필요한 개수만큼 장소 추가
        int added = 0;
        
        for (DataEntity data : topRatedPlaces) {
            if (added >= needed) break;
            
            String norm = normalize(data.getTitle());
            if (usedNames.contains(norm)) continue;
            if (data.getC1Code().equals("AC")) continue; // 숙박 제외
            
            usedNames.add(norm);
            
            ObjectNode node = mapper.createObjectNode();
            node.put("region", region);
            node.put("ward", ward);
            node.put("id", data.getId());
            node.put("title", data.getTitle());
            node.put("mapx", Double.parseDouble(data.getMapx()));
            node.put("mapy", Double.parseDouble(data.getMapy()));
            node.put("contentId", data.getContentId());
            node.put("firstimage", data.getFirstimage());
            node.put("firstimage2", data.getFirstimage2());
            node.put("addr1", data.getAddr1());
            node.put("addr2", data.getAddr2());
            node.put("c1Code", data.getC1Code());
            node.put("c2Code", data.getC2Code());
            node.put("c3Code", data.getC3Code());
            node.put("tel", data.getTel());
            node.put("contentTypeId", data.getTheme());
            node.put("theme", data.getTheme());
            node.put("regionName", data.getRegionCodeEntity().getName());
            node.put("regionCode", data.getRegionCodeEntity().getRegioncode());
            node.put("wardName", data.getWardCodeEntity().getName());
            node.put("wardCode", data.getWardCodeEntity().getWardcode());
            node.put("viewCount", data.getViewCount());
            node.put("rating", data.getRating());
            node.put("reviewCount", data.getReviewCount());
            node.put("likeCount", data.getLikeCount());
            
            result.add(node);
            added++;
            System.out.println("DB에서 추가된 장소 (score: " + data.getScore() + "): " + data.getTitle());
        }
        
        System.out.println("실제 추가된 장소 수: " + added + " / 필요한 수: " + needed);
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
     */
	@Transactional(readOnly = true)
    public List<JsonNode> getFilteredAndFilledPlaces(JsonNode gptArray, int gptCount, List<Integer> themeIds) {
        System.out.println("=== GPT 원본 일정 개수: " + gptArray.size() + " ===");
        
        List<JsonNode> filtered = filterExistingPlaces(gptArray, themeIds);
        System.out.println("=== DB에서 매칭된 장소 개수: " + filtered.size() + " ===");

        // 1. 원본 GPT에서 day-order -> place 매핑
        Map<String, JsonNode> gptDayOrderMap = new HashMap<>();
        
        for (JsonNode place : gptArray) {
            String key = place.get("day").asInt() + "-" + place.get("order").asInt();
            gptDayOrderMap.put(key, place);
            System.out.println("GPT 원본: " + key + " -> " + place.get("title").asText());
        }

        // 2. 필터링된 결과에서 있는 day-order 확인
        Set<String> existingKeys = filtered.stream()
                .map(p -> p.get("day").asInt() + "-" + p.get("order").asInt())
                .collect(Collectors.toSet());

        System.out.println("매칭된 키들: " + existingKeys);

        // 3. 누락된 자리 목록 수집
        List<String> missingKeys = new ArrayList<>();
        List<JsonNode> missingGptPlaces = new ArrayList<>();
        for (Map.Entry<String, JsonNode> entry : gptDayOrderMap.entrySet()) {
            if (!existingKeys.contains(entry.getKey())) {
                missingKeys.add(entry.getKey());
                missingGptPlaces.add(entry.getValue());
                System.out.println("누락된 장소: " + entry.getKey() + " -> " + entry.getValue().get("title").asText());
            }
        }
        
        System.out.println("=== 누락된 장소 개수: " + missingKeys.size() + " ===");
        
        // region/ward 기반으로 누락된 위치 보완
        List<JsonNode> dbFilled = new ArrayList<>();
        
        if (!missingGptPlaces.isEmpty()) {
            // 첫 번째 GPT 장소에서 region/ward 정보 추출
            JsonNode firstPlace = gptArray.get(0);
            String region = normalizeRegionName(firstPlace.get("region").asText());
            String ward = firstPlace.get("ward").asText();
            
            System.out.println("보완할 지역: " + region + " " + ward);

            // 누락된 자리만큼 DB에서 보완 (score 높은 순)
            dbFilled = fillWithDbPlacesOnly(
                missingKeys,
                region,
                ward,
                themeIds
            );

            System.out.println("=== DB에서 보완된 장소 개수: " + dbFilled.size() + " ===");

            // 보완된 DB 장소를 누락된 GPT 자리의 day/order에 정확히 맞춰줌
            for (int i = 0; i < dbFilled.size() && i < missingGptPlaces.size(); i++) {
                ObjectNode filled = (ObjectNode) dbFilled.get(i);
                JsonNode original = missingGptPlaces.get(i);
                filled.put("day", original.get("day").asInt());
                filled.put("order", original.get("order").asInt());
                System.out.println("보완 매칭: " + original.get("day").asInt() + "-" + original.get("order").asInt() 
                    + " -> " + filled.get("title").asText());
            }
        }
        
        // 최종 합치기 (기존 유지 + 보완)
        List<JsonNode> finalList = new ArrayList<>(filtered);
        finalList.addAll(dbFilled);

        System.out.println("=== 합친 후 총 장소 개수: " + finalList.size() + " ===");

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
        
        System.out.println("=== 최종 반환 장소 개수: " + dedupedList.size() + " ===");
        for (JsonNode place : dedupedList) {
            System.out.println("최종: " + place.get("day").asInt() + "-" + place.get("order").asInt() 
                + " -> " + place.get("title").asText() + " (score: " + 
                (place.has("score") ? place.get("score").asText() : "N/A") + ")");
        }
        
        return dedupedList;
    }
}