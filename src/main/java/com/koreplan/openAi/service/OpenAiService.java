package com.koreplan.openAi.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
//            String gptPrompt = """
//            		당신은 여행 일정 플래너입니다. 다음 조건에 맞는 일정(JSON 배열)만 생성하세요: 지역: %s, 여행일 수: %d일, 동행 유형: %s, 선호 장소: %s
//            		중요 조건: 각 날짜별로 최소 3개 이상의 장소를 추천하세요! & 각 장소마다 정확한 주소를 포함하세요 & 같은 주소의 장소는 중복 추천하지 마세요 
//            		& 1박 2일인 경우: Day 1에 3개, Day 2에 3개 (총 6개) - 2박 3일인 경우: Day 1에 3개, Day 2에 4개, Day 3에 3개 (총 10개) & 3박 이상인 경우: 첫날/마지막날 3개씩, 중간 날짜들은 4개씩
//            		장소 간 이동 거리는 짧게 구성하고, 같은 구(ward) 내의 장소들로 추천하세요. & 데이터는 추천도를 기준으로 정렬해주세요.
//            		응답 형식: 꼭 JSON 배열만 출력하세요. 코드블록(```json 등)은 절대 포함하지 마세요.
//            		예시: [{"day": 1, "order": 1, "region": "서울특별시", "ward": "종로구", "title": "경복궁", "address": "서울특별시 종로구 세종대로 175", "mapx": 127.xxx, "mapy":37.xxxx}, ]
//            		""".formatted(region, days, companion, preferences);
            
            String gptPrompt = String.format("""
					여행 일정 JSON 배열만 생성, 코드블록(```json 등) 금지
					조건: 지역=%s, 일수=%d, 동행=%s, 선호=%s  
					요구:
					- 각 장소에 정확한 주소 포함  
					- 같은 주소 중복 금지  
					- 일정별 장소 수: 1박2일[3,3], 2박3일[3,4,3], 3박 이상[첫·끝 3개, 나머지 4개]  
					- 장소는 같은 구(ward) 내, 이동거리 짧게  
					- 추천도 순 정렬  
					JSON 배열 외 다른 출력 금지 
					응답 예시: [{"day":1,"order":1,"region":"서울특별시","ward":"종로구","title":"경복궁","address":"서울특별시 종로구 세종대로 175","mapx":127.xxx,"mapy":37.xxx}, ...]
					""", region, days, companion, preferences);

            int estimatedInputTokens = gptPrompt.length() / 4;
            int estimatedOutputTokens = 200;

            if (!usageTracker.canProceed(estimatedInputTokens, estimatedOutputTokens)) {
                return "월 사용 예산을 초과하였습니다. (예산: $5)";
            }

            // 3. 안전하게 JSON 생성
            ObjectNode requestNode = mapper.createObjectNode();
            requestNode.put("model", "gpt-4o");
            requestNode.put("max_tokens", 2250); // 토큰 제한 추가
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
        case "서울", "서울특별시" -> "서울특별시";
        case "부산", "부산광역시" -> "부산광역시";
        case "대구", "대구광역시" -> "대구광역시";
        case "인천", "인천광역시" -> "인천광역시";
        case "광주", "광주광역시" -> "광주광역시";
        case "대전", "대전광역시" -> "대전광역시";
        case "울산", "울산광역시" -> "울산광역시";
        case "세종", "세종시", "세종특별자치시" -> "세종특별자치시";
        case "경기", "경기도" -> "경기도";
        case "강원", "강원도", "강원특별자치도" -> "강원특별자치도";
        case "충북", "충청북도" -> "충청북도";
        case "충남", "충청남도" -> "충청남도";
        case "전북", "전북특별자치도", "전라북도" -> "전북특별자치도";
        case "전남", "전라남도" -> "전라남도";
        case "경북", "경상북도" -> "경상북도";
        case "경남", "경상남도" -> "경상남도";
        case "제주", "제주도" , "제주특별자치도"-> "제주특별자치도";
        default -> region;
	    };
	}
	
	// 주소 정규화 (구 + 주요 도로명 추출)
	private String normalizeAddress(String address) {
	    if (address == null) return "";
	    
	    // "서울특별시 종로구 세종대로 175" → "종로구 세종대로"
//	    Pattern pattern = Pattern.compile("(\\w+구)\\s+(\\w+[로길동])");
	    Pattern pattern = Pattern.compile("(\\w+구)\\s+([\\w\\d]+[로길동가])");
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
	    
//	    boolean result = norm1.equals(norm2);
	    boolean result = norm1.equals(norm2) || norm1.contains(norm2) || norm2.contains(norm1);
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
	
	
	
	//// 거리보정
	private static final Set<String> URBAN_AREAS = Set.of(
		    "서울특별시", "부산광역시", "대구광역시", "인천광역시",
		    "광주광역시", "대전광역시", "울산광역시"
	);

	public boolean isUrbanArea(String regionName) {
	    return URBAN_AREAS.contains(regionName);
	}

	// 하버사인 공식으로 두 좌표 간 거리 계산 (km 단위)
	public static double haversine(double lon1, double lat1, double lon2, double lat2) {
	    final int R = 6371; // 지구 반경 (km)
	    double dLat = Math.toRadians(lat2 - lat1);
	    double dLon = Math.toRadians(lon2 - lon1);
	    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
	             + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
	             * Math.sin(dLon / 2) * Math.sin(dLon / 2);
	    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	    return R * c;
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
	public List<JsonNode> filterExistingPlaces(JsonNode gptArray, List<Integer> themeIds, Set<Long> usedIds) {
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
	        	if (usedIds.contains(data.getId())) continue; 
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
    	            usedIds.add(data.getId()); 
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
    public List<JsonNode> fillWithDbPlacesOnly(List<String> missingKeys, String region, String ward, List<Integer> themeIds,
    		 double baseMapx, double baseMapy, Set<Long> usedIds) {
        
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

        // ★ 수정: 넉넉히 3배수 만큼 먼저 score 높은 순 조회
        int needed = missingKeys.size();
        

        // ★ 수정: 도심/시골 최대거리 분기 적용
        double maxDistanceKm = isUrbanArea(region) ? 5.0 : 35.0;
        
        int fetchSize = needed * 3;
        Pageable pageable = PageRequest.of(0, fetchSize, Sort.by(Sort.Direction.DESC, "score"));
        List<DataEntity> topRatedPlaces = dataRepository.findByRegionCodeEntityAndWardCodeEntityAndThemeIn(
            regionEntity, wardEntity, themeIds, pageable
        ).getContent();
        
        System.out.println("DB에서 조회된 상위 " + needed + "개 장소 (score 높은 순): " + topRatedPlaces.size());
        System.out.println("최대 거리 제한: " + maxDistanceKm + " km");
        
        // 중복 방지용
        Set<String> usedNames = new HashSet<>();
        List<JsonNode> result = new ArrayList<>();

        // 필요한 개수만큼 장소 추가
        int added = 0;
        
        
        for (DataEntity data : topRatedPlaces) {
//            if (added >= needed) break;
//            
//            String norm = normalize(data.getTitle());
//            if (usedNames.contains(norm)) continue;
//            if (data.getC1Code().equals("AC")) continue; // 숙박 제외
//            
//            usedNames.add(norm);
        	
//            if (added >= needed) break;
//            if (data.getC1Code().equals("AC")) continue; // 숙박 제외
//            // ★ 수정: 하버사인 거리 계산 후 거리 필터링
//            double dist = haversine(baseMapx, baseMapy, Double.parseDouble(data.getMapx()), Double.parseDouble(data.getMapy()));
//            if (dist > maxDistanceKm) continue;
//            String norm = normalize(data.getTitle());
//            if (usedNames.contains(norm)) continue;
//            usedNames.add(norm);
//            if (usedIds.contains(data.getId())) continue; // ✅ 이미 사용된 장소는 제외
//        	
        	  double dist = haversine(baseMapx, baseMapy, Double.parseDouble(data.getMapx()), Double.parseDouble(data.getMapy()));
        	    String norm = normalize(data.getTitle());
        	    boolean isDuplicateId = usedIds.contains(data.getId());
        	    boolean isAccommodation = "AC".equals(data.getC1Code());

        	    System.out.println("📌 후보: " + data.getTitle());
        	    System.out.println(" - 거리: " + dist + "km");
        	    System.out.println(" - 중복ID: " + isDuplicateId);
        	    System.out.println(" - 숙박여부: " + isAccommodation);

        	    if (added >= needed) break;

        	    if (isDuplicateId) continue;

        	    if (data.getC1Code().equals("AC")) {
        	        System.out.println("🛏️ 숙박 제외: " + data.getTitle());
        	        continue;
        	    }

        	    if (dist > maxDistanceKm) {
        	        if (result.isEmpty() && dist <= 60.0) {
        	            System.out.println("⚠️ fallback 거리 허용: " + data.getTitle());
        	        } else {
        	            continue;
        	        }
        	    }
        	
        	
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
            usedIds.add(data.getId());

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
//	@Transactional(readOnly = true)
//    public List<JsonNode> getFilteredAndFilledPlaces(JsonNode gptArray, int gptCount, List<Integer> themeIds) {
//        System.out.println("=== GPT 원본 일정 개수: " + gptArray.size() + " ===");
//        
//        List<JsonNode> filtered = filterExistingPlaces(gptArray, themeIds);
//        System.out.println("=== DB에서 매칭된 장소 개수: " + filtered.size() + " ===");
//
//        // 1. 원본 GPT에서 day-order -> place 매핑
//        Map<String, JsonNode> gptDayOrderMap = new HashMap<>();
//        
//        for (JsonNode place : gptArray) {
//            String key = place.get("day").asInt() + "-" + place.get("order").asInt();
//            gptDayOrderMap.put(key, place);
//            System.out.println("GPT 원본: " + key + " -> " + place.get("title").asText());
//        }
//
//        // 2. 필터링된 결과에서 있는 day-order 확인
//        Set<String> existingKeys = filtered.stream()
//                .map(p -> p.get("day").asInt() + "-" + p.get("order").asInt())
//                .collect(Collectors.toSet());
//
//        System.out.println("매칭된 키들: " + existingKeys);
//
//        // 3. 누락된 자리 목록 수집
//        List<String> missingKeys = new ArrayList<>();
//        List<JsonNode> missingGptPlaces = new ArrayList<>();
//        for (Map.Entry<String, JsonNode> entry : gptDayOrderMap.entrySet()) {
//            if (!existingKeys.contains(entry.getKey())) {
//                missingKeys.add(entry.getKey());
//                missingGptPlaces.add(entry.getValue());
//                System.out.println("누락된 장소: " + entry.getKey() + " -> " + entry.getValue().get("title").asText());
//            }
//        }
//        
//        System.out.println("=== 누락된 장소 개수: " + missingKeys.size() + " ===");
//        
//        // region/ward 기반으로 누락된 위치 보완
//        List<JsonNode> dbFilled = new ArrayList<>();
//        
//        if (!missingGptPlaces.isEmpty()) {
//            // 첫 번째 GPT 장소에서 region/ward 정보 추출
//            JsonNode firstPlace = gptArray.get(0);
//            String region = normalizeRegionName(firstPlace.get("region").asText());
//            String ward = firstPlace.get("ward").asText();
//            
//            System.out.println("보완할 지역: " + region + " " + ward);
//            
//            // ✅ 수정: 중복된 id 제외용 Set 추가
//            Set<Long> usedIds = new HashSet<>();
//            for (JsonNode node : filtered) {
//                if (node.has("id")) {
//                    usedIds.add(node.get("id").asLong());
//                }
//            }
//            
//            // 보완된 장소를 하나씩 거리 기준으로 채우기
//            for (int i = 0; i < missingKeys.size(); i++) {
//                String missKey = missingKeys.get(i);
//                JsonNode original = missingGptPlaces.get(i);
//                
//                // 해당 슬롯의 좌표를 기준으로 거리 보정
//                double baseMapx = original.get("mapx").asDouble();
//                double baseMapy = original.get("mapy").asDouble();
//
//                List<JsonNode> oneFilled = fillWithDbPlacesOnly(
//                    List.of(missKey),
//                    region,
//                    ward,
//                    themeIds,
//                    baseMapx,
//                    baseMapy,
//                    usedIds
//                );
//
//                if (!oneFilled.isEmpty()) {
//                    ObjectNode filled = (ObjectNode) oneFilled.get(0);
//
//                    // 원래의 day/order 덮어쓰기
//                    filled.put("day", original.get("day").asInt());
//                    filled.put("order", original.get("order").asInt());
//
//                    // 중복 방지용 ID 등록
//                    if (filled.has("id")) {
//                        usedIds.add(filled.get("id").asLong());
//                    }
//
//                    dbFilled.add(filled);
//
//                    System.out.println("보완 매칭(개별): " + original.get("day").asInt()
//                        + "-" + original.get("order").asInt()
//                        + " → " + filled.get("title").asText());
//                }
//            }
//
//
//            System.out.println("=== DB에서 보완된 장소 개수: " + dbFilled.size() + " ===");
//
//            // 보완된 DB 장소를 누락된 GPT 자리의 day/order에 정확히 맞춰줌
////            for (int i = 0; i < dbFilled.size() && i < missingGptPlaces.size(); i++) {
////                ObjectNode filled = (ObjectNode) dbFilled.get(i);
////                JsonNode original = missingGptPlaces.get(i);
////                filled.put("day", original.get("day").asInt());
////                filled.put("order", original.get("order").asInt());
////                System.out.println("보완 매칭: " + original.get("day").asInt() + "-" + original.get("order").asInt() 
////                    + " -> " + filled.get("title").asText());
////            }
//        }
//        
//        // 최종 합치기 (기존 유지 + 보완)
//        List<JsonNode> finalList = new ArrayList<>(filtered);
//        finalList.addAll(dbFilled);
//
//        System.out.println("=== 합친 후 총 장소 개수: " + finalList.size() + " ===");
//
//        // 중복 제거 (같은 day-order는 하나만)
////        Map<String, JsonNode> uniqueMap = new LinkedHashMap<>();
////        
////        
////        for (JsonNode place : finalList) {
////            String key = place.get("day").asInt() + "-" + place.get("order").asInt();
////            uniqueMap.put(key, place);
////        }
////
////        List<JsonNode> dedupedList = new ArrayList<>(uniqueMap.values());
////        dedupedList.sort(Comparator
////            .comparingInt(n -> ((JsonNode) n).get("day").asInt())
////            .thenComparingInt(n -> ((JsonNode) n).get("order").asInt()));
////        
////        System.out.println("=== 최종 반환 장소 개수: " + dedupedList.size() + " ===");
////        for (JsonNode place : dedupedList) {
////            System.out.println("최종: " + place.get("day").asInt() + "-" + place.get("order").asInt() 
////                + " -> " + place.get("title").asText() + " (score: " + 
////                (place.has("score") ? place.get("score").asText() : "N/A") + ")");
////        }
//        // ✅ 수정: Map 제거하고 중복 제거 for문으로 대체
//        List<JsonNode> dedupedList = new ArrayList<>();
//        Set<String> seenKeys = new HashSet<>();
//
//        for (JsonNode place : finalList) {
//            String key = place.get("day").asInt() + "-" + place.get("order").asInt();
//            if (!seenKeys.contains(key)) {
//                seenKeys.add(key);
//                dedupedList.add(place);
//            }
//        }
//
//        dedupedList.sort(Comparator
//            .comparingInt(n -> ((JsonNode) n).get("day").asInt())
//            .thenComparingInt(n -> ((JsonNode) n).get("order").asInt()));
//
//        System.out.println("=== 최종 반환 장소 개수: " + dedupedList.size() + " ===");
//        for (JsonNode place : dedupedList) {
//            System.out.println("최종: " + place.get("day").asInt() + "-" + place.get("order").asInt() 
//                + " -> " + place.get("title").asText() + " (score: " + 
//                (place.has("score") ? place.get("score").asText() : "N/A") + ")");
//        }
//
//        return dedupedList;
//	}
    
    @Transactional(readOnly = true)
    public List<JsonNode> getFilteredAndFilledPlaces(JsonNode gptArray, int gptCount, List<Integer> themeIds) {
        System.out.println("=== GPT 원본 일정 개수: " + gptArray.size() + " ===");
        
//        List<JsonNode> filtered = filterExistingPlaces(gptArray, themeIds);
//        System.out.println("=== DB에서 매칭된 장소 개수: " + filtered.size() + " ===");

        // 1. 원본 GPT에서 day-order -> place 매핑 (map 대신 LinkedHashMap 사용해 순서 보장)
        Map<String, ObjectNode> finalMap = new LinkedHashMap<>();

        // 2. 중복 방지용 DB 장소 id 집합 생성
        Set<Long> usedIds = new HashSet<>();
        List<JsonNode> filtered = filterExistingPlaces(gptArray, themeIds, usedIds);
        System.out.println("=== DB에서 매칭된 장소 개수: " + filtered.size() + " ===");
        for (JsonNode node : filtered) {
            if (node.has("id")) {
                usedIds.add(node.get("id").asLong());
            }
        }

        // 3. GPT 원본 배열 전체 순회하면서
        for (int i = 0; i < gptArray.size(); i++) {
            JsonNode gptPlace = gptArray.get(i);
            // ✅ GPT 원본 중복 제거
            if (gptPlace.has("id")) {
                long gptId = gptPlace.get("id").asLong();
                if (usedIds.contains(gptId)) {
                    System.out.println("⚠️ 중복 장소 ID 스킵됨: " + gptPlace.get("title").asText());
                    continue; // 중복이면 스킵
                }
            }
            
            int day = gptPlace.get("day").asInt();
            int order = gptPlace.get("order").asInt();
            String key = day + "-" + order;

            // 4. filtered 리스트에서 같은 day-order 찾기 (람다 없이 for문)
            ObjectNode matchedNode = null;
            for (JsonNode fnode : filtered) {
                int fday = fnode.get("day").asInt();
                int forder = fnode.get("order").asInt();
                if (fday == day && forder == order) {
                    matchedNode = (ObjectNode) fnode;
                    break;
                }
            }

            if (matchedNode != null) {
                // DB에서 매칭된 장소가 있으면 그대로 사용
                finalMap.put(key, matchedNode);
            } else {
                // 매칭 안 된 경우 DB에서 보완
                String region = normalizeRegionName(gptPlace.get("region").asText());
                String ward = gptPlace.get("ward").asText();
                double mapx = gptPlace.get("mapx").asDouble();
                double mapy = gptPlace.get("mapy").asDouble();

                List<JsonNode> filledList = fillWithDbPlacesOnly(
                    List.of(key),
                    region,
                    ward,
                    themeIds,
                    mapx,
                    mapy,
                    usedIds
                );

                if (filledList != null && !filledList.isEmpty()) {
                    ObjectNode filled = (ObjectNode) filledList.get(0);
                    // day, order 무조건 유지
                    filled.put("day", day);
                    filled.put("order", order);
                    // 중복 방지용 id 등록
                    if (filled.has("id")) {
                        usedIds.add(filled.get("id").asLong());
                    }
                    finalMap.put(key, filled);

                    System.out.println("보완 매칭: " + key + " → " + filled.get("title").asText());
                } else {
                    // DB에서 보완 실패하면 경고 출력 (무조건 DB에서 채워야 하므로 실패 방지 중요)
                    System.err.println("❌ 보완 실패: " + key + " - region: " + region + ", ward: " + ward);
                }
            }
        }

        // 5. 최종 결과 리스트 생성 및 정렬
        List<JsonNode> finalList = new ArrayList<>(finalMap.values());
        finalList.sort(new Comparator<JsonNode>() {
            @Override
            public int compare(JsonNode o1, JsonNode o2) {
                int day1 = o1.get("day").asInt();
                int day2 = o2.get("day").asInt();
                if (day1 != day2) {
                    return day1 - day2;
                }
                int order1 = o1.get("order").asInt();
                int order2 = o2.get("order").asInt();
                return order1 - order2;
            }
        });

        System.out.println("=== 최종 반환 장소 개수: " + finalList.size() + " ===");
        for (JsonNode place : finalList) {
            System.out.println("최종: " + place.get("day").asInt() + "-" + place.get("order").asInt()
                + " -> " + place.get("title").asText()
                + " (score: " + (place.has("score") ? place.get("score").asText() : "N/A") + ")");
        }

        return finalList;
    }
    
}