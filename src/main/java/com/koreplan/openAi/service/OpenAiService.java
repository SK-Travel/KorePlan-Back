package com.koreplan.openAi.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
					여행 일정 플래너
					조건: 지역=%s, 일수=%d, 동행=%s, 선호=%s  
					요구:
					- 각 장소에 정확한 주소 포함  
					- 같은 주소 중복 금지  
					- 일정별 장소 수: 1박2일[3,2], 2박3일[3,4,2], 3박 이상[첫날 3개, 마지막날 2개, 나머지 4개]  
					- 장소는 같은 구(ward) 내, 이동거리 짧게  
					- 추천도 순 정렬
					- 각 날의 마지막 일정에 무조건 동일한 호텔 한 곳을 방문지로 포함
					-JSON 배열 외 다른 코드블록(```json 등) 출력 금지 
					응답 예시: [{"day":1,"order":1,"region":"서울특별시","ward":"종로구","title":"경복궁","address":"서울특별시 종로구 세종대로 175","mapx":127.xxx,"mapy":37.xxx}, ...{ "day": 1, "order": 4, "region": "서울특별시", "ward": "중구", "title": "롯데호텔서울", "address": "서울특별시 중구 을지로 30 롯데호텔", "mapx": 127.xxx, "mapy": 37.xxx },]
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
            e.printStackTrace(); // 콘솔에 전체 예외 스택 추적 출력
            return "OPEN AI 응답 처리 중 오류 내용: " + e.toString(); // getMessage() 대신 toString() 사용

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
	
	 //주소 정규화 (구 + 주요 도로명 추출)
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
	

	private boolean isMatchingPlace(String gptTitle, String gptAddress, String dbTitle, String dbAddr1) {
	    boolean addressMatch = isSimilarAddress(gptAddress, dbAddr1);
	    boolean titleMatch = isSimilarTitle(gptTitle, dbTitle);

	    if(addressMatch || titleMatch) {
	        System.out.println("→ 매칭 성공 (주소: " + addressMatch + ", 타이틀: " + titleMatch + ")");
	        return true;
	    } else {
	        System.out.println("→ 매칭 실패");
	        return false;
	    }
	}
	
	
	
	//// 거리보정
	private static final Set<String> URBAN_AREAS = Set.of(
		    "서울특별시", "부산광역시", "대구광역시", "인천광역시",
		    "광주광역시", "대전광역시", "울산광역시", "제주특별자치도", "세종특별자치시"
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
	
	
//	// 숙소 추출 함수 추가
//	private JsonNode extractHotel(JsonNode gptArray) {
//	    for (JsonNode place : gptArray) {
//	        String title = place.get("title").asText("").toLowerCase();
//	        if (title.contains("호텔") || title.contains("guest") || title.contains("숙소") || title.contains("모텔")) {
//	            return place;
//	        }
//	    }
//	    return null;
//	}
	
	
	// 필터링 로직 - 주소 기반 매칭 적용
	@Transactional(readOnly = true)
	public List<JsonNode> filterExistingPlaces(JsonNode gptArray, List<Integer> themeIds, Set<Long> usedIds) {
		List<JsonNode> result = new ArrayList<>();
		
		// 먼저 중복 주소 제거
		List<JsonNode> uniquePlaces = removeDuplicateAddresses(gptArray);
		
		System.out.println("📌 필터링 시작: GPT 원본=" + gptArray.size() + " / 중복 제거 후=" + uniquePlaces.size());

		
		for (JsonNode place : uniquePlaces) {
			String regionName = normalizeRegionName(place.get("region").asText());
	        String wardName = place.has("ward") ? place.get("ward").asText() : null;
	        String placeName = place.has("title") ? place.get("title").asText() : null;
	        String placeAddress = place.has("address") ? place.get("address").asText() : null;
	        
	        System.out.println("🔎 시도: " + placeName + " (" + regionName + " " + wardName + ")");

	        if (wardName == null || placeName == null) {
	            System.out.println("❌ ward/title null로 스킵됨");
	            continue;
	        }

	        // 1. regioncode 조회
	        Optional<RegionCodeEntity> regionOpt = regionCodeRepository.findRegionByNameForAI(regionName);
	        if (regionOpt.isEmpty()) {
	            System.out.println("❌ 지역 미일치: " + regionName);
	            continue;
	        }
	        RegionCodeEntity regionEntity = regionOpt.get();
	      
	        // 2. ward 이름과 region 조합으로 WardCodeEntity 조회
	        Optional<WardCodeEntity> wardOpt = wardCodeRepository.findWardByNameAndRegionForAI(wardName, regionEntity);
	        if (wardOpt.isEmpty()) {
	            System.out.println("❌ 구 미일치: " + wardName);
	            continue;
	        }
	        WardCodeEntity wardEntity = wardOpt.get();

	        // 3. 주소 + 타이틀 기반 매칭 (DB의 addr1만 사용)
	        List<DataEntity> candidates = dataRepository.findByRegionCodeEntityAndWardCodeEntityAndThemeIn(regionEntity, wardEntity, themeIds);
	        System.out.println("🎯 후보 개수: " + candidates.size());
	        for (DataEntity data : candidates) {
	        	System.out.println("[후보] " + data.getTitle());
//	        	if (usedIds.contains(data.getId())) continue; 
	        	if (usedIds.contains(data.getId())) {
	        	    if ("AC".equals(data.getC1Code())) {
	        	        System.out.println("✅ 숙소 중복 허용 (filterExistingPlaces): " + data.getTitle());
	        	    } else {
	        	        continue;
	        	    }
	        	}
	        	
	            if (isMatchingPlace(placeName, placeAddress, data.getTitle(), data.getAddr1())) {
	            	if (data.getC1Code().equals("AC")) {
	            		System.out.println("⛔ 숙소는 여기선 제외");
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
    	            System.out.println("✅ 매칭 성공: " + placeName + " ↔ " + data.getTitle());
	                break; // 매칭되었으므로 반복 종료
	            }
	        }
		}
		System.out.println("✅ 최종 필터링 결과: " + result.size() + "개 매칭됨");
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
            System.out.println("❌ 지역 매칭 실패: " + region);
            return List.of();
        }

        WardCodeEntity wardEntity = wardCodeRepository.findWardByNameAndRegionForAI(ward, regionEntity).orElse(null);
        if (wardEntity == null) {
            System.out.println("❌ 구 매칭 실패: " + ward);
            return List.of();
        }


        // ★ 수정: 넉넉히 3배수 만큼 먼저 score 높은 순 조회
        int needed = missingKeys.size();
        

        // ★ 수정: 도심/시골 최대거리 분기 적용
        double maxDistanceKm = isUrbanArea(region) ? 20.0 : 70.0;
        
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
    private String normalizeText(String text) {
        if (text == null) return "";
        return text.toLowerCase().replaceAll("[\\s\\p{Punct}]", ""); // 소문자, 공백 및 특수문자 제거
    }

    @Transactional(readOnly = true)
    public List<JsonNode> getFilteredAndFilledPlaces(JsonNode gptArray, int gptCount, List<Integer> themeIds) {
        System.out.println("=== GPT 원본 일정 개수: " + gptArray.size() + " ===");

        // 1. 중복 체크용 ID 집합
        Set<Long> usedIds = new HashSet<>();

        // 2. 숙소 여러 개 찾기 (★ 수정됨)
        List<JsonNode> hotels = findAllAccommodationsFromGptOrDb(gptArray, usedIds);
        Map<String, JsonNode> hotelMap = new HashMap<>();
        for (JsonNode hotel : hotels) {
            String key = hotel.get("day").asInt() + "-" + hotel.get("order").asInt();
            hotelMap.put(key, hotel);
        }
        
     // 여기 바로 추가
        Map<Integer, JsonNode> dayToHotel = new HashMap<>();
        for (JsonNode hotel : hotelMap.values()) {
            int day = hotel.get("day").asInt();
            if (!dayToHotel.containsKey(day)) {
                dayToHotel.put(day, hotel);  // 날짜별 대표 숙소 1개만 저장
            }
        }

        // 3. 숙소 테마(32) 제외하고 themeIds 필터링 (숙소는 따로 처리)
        List<Integer> filteredThemeIds = new ArrayList<>();
        for (Integer t : themeIds) {
            if (t != 32) filteredThemeIds.add(t);
        }

        // 4. GPT 일정에서 숙소 제외하고 DB 필터링 장소 가져오기
        List<JsonNode> filtered = filterExistingPlaces(gptArray, filteredThemeIds, usedIds);
        for (JsonNode node : filtered) {
            if (node.has("id")) usedIds.add(node.get("id").asLong());
        }
        
        // ★ 여기에 숙소 ID 추가
        for (JsonNode hotel : hotelMap.values()) {
            if (hotel.has("id")) usedIds.add(hotel.get("id").asLong());
        }
        
        // 5. 결과 담을 LinkedHashMap (day-order 순서 보장)
        Map<String, ObjectNode> finalMap = new LinkedHashMap<>();

        // 6. GPT 원본 전체 순회하며 최종 장소 선택
        for (int i = 0; i < gptArray.size(); i++) {
            JsonNode gptPlace = gptArray.get(i);
            int day = gptPlace.get("day").asInt();
            int order = gptPlace.get("order").asInt();
            String key = day + "-" + order;

            // 6-1. 숙소가 있다면 해당 key에 숙소 넣기 (★ 수정됨)
            if (hotelMap.containsKey(key)) {
                finalMap.put(key, (ObjectNode) hotelMap.get(key));
                continue;
            }

            // 6-2. 중복 id 체크 (단, 숙소 id는 중복 허용) (★ 수정됨)
            if (gptPlace.has("id")) {
                long gptId = gptPlace.get("id").asLong();
                boolean isHotelId = hotelMap.values().stream()
                    .anyMatch(h -> h.has("id") && h.get("id").asLong() == gptId);
                if (!isHotelId && usedIds.contains(gptId)) {
                    System.out.println("⚠️ 중복 장소 ID 스킵됨: " + gptPlace.get("title").asText());
                    continue;
                }
            }

            // 6-3. filtered 리스트에서 day-order 일치하는 장소 찾기 (DB 매칭)
            ObjectNode matchedNode = null;
            for (JsonNode fnode : filtered) {
                if (fnode.get("day").asInt() == day && fnode.get("order").asInt() == order) {
                    matchedNode = (ObjectNode) fnode;
                    break;
                }
            }

            if (matchedNode != null) {
                finalMap.put(key, matchedNode);
                if (gptPlace instanceof ObjectNode && matchedNode.has("id")) {
                    ((ObjectNode) gptPlace).put("id", matchedNode.get("id").asLong());
                    usedIds.add(matchedNode.get("id").asLong());
                }
            } else {
                // 6-4. DB에서 못 찾으면 위치 기반으로 채우기
                String region = normalizeRegionName(gptPlace.get("region").asText());
                String ward = gptPlace.get("ward").asText();
                double mapx = gptPlace.get("mapx").asDouble();
                double mapy = gptPlace.get("mapy").asDouble();

                List<JsonNode> filledList = fillWithDbPlacesOnly(
                    List.of(key),
                    region,
                    ward,
                    filteredThemeIds,
                    mapx,
                    mapy,
                    usedIds
                );

                if (filledList != null && !filledList.isEmpty()) {
                    ObjectNode filled = (ObjectNode) filledList.get(0);
                    filled.put("day", day);
                    filled.put("order", order);
                    if (filled.has("id")) usedIds.add(filled.get("id").asLong());
                    finalMap.put(key, filled);

                    System.out.println("보완 매칭: " + key + " → " + filled.get("title").asText());
                } else {
                    System.err.println("❌ 보완 실패: " + key + " - region: " + region + ", ward: " + ward);
                }
            }
        }
        // 7. 각 day 마지막에 숙소 복귀 추가 (마지막 날 제외, 중간 숙소 제거)
        if (!hotelMap.isEmpty()) {
        	// 7-1. 날짜별 max order 계산
        	Map<Integer, Integer> maxOrderByDay = new HashMap<>();
        	for (JsonNode node : finalMap.values()) {
        	    int day = node.get("day").asInt();
        	    int order = node.get("order").asInt();
        	    maxOrderByDay.put(day, Math.max(maxOrderByDay.getOrDefault(day, 0), order));
        	}

        	// 7-2. 날짜별 숙소 하나만 꺼내는 맵 대신, 대표 숙소 하나 선택
        	JsonNode commonHotel = null;
        	for (JsonNode hotel : hotelMap.values()) {
        	    commonHotel = hotel;
        	    break; // 첫 번째 숙소 하나만 선택
        	}


        	// 7-3. 중간 숙소 제거 (order < 숙소 마지막 order인 경우)
        	Iterator<Map.Entry<String, ObjectNode>> iter = finalMap.entrySet().iterator();
        	while (iter.hasNext()) {
        	    Map.Entry<String, ObjectNode> entry = iter.next();
        	    ObjectNode node = entry.getValue();
        	    int day = node.get("day").asInt();
        	    int order = node.get("order").asInt();

        	    for (JsonNode hotel : hotelMap.values()) {
        	        if (node.has("title") && hotel.has("title")
        	            && node.get("title").asText().equals(hotel.get("title").asText())
        	            && node.has("address") && hotel.has("address")
        	            && node.get("address").asText().equals(hotel.get("address").asText())
        	            && day == hotel.get("day").asInt()) {

        	            int maxHotelOrder = maxOrderByDay.getOrDefault(day, -1);
        	            if (order < maxHotelOrder) {
        	                iter.remove();
        	                System.out.println("❌ 중간 숙소 제거 (order < 숙소 마지막 order): day " + day + ", order " + order);
        	                break;
        	            }
        	        }
        	    }
        	}
        	// 7-4. 마지막 날 제외하고 각 날짜 마지막에 숙소 추가
        	int lastDay = Collections.max(maxOrderByDay.keySet());

        	for (int day : maxOrderByDay.keySet()) {
        	    if (day == lastDay) continue;

        	    int maxOrder = maxOrderByDay.get(day);
        	    String lastKey = day + "-" + maxOrder;
        	    JsonNode lastNode = finalMap.get(lastKey);

        	    // 이미 숙소인지 확인 (addr1 기준)
        	    boolean alreadyLastIsHotel = false;
        	    if (lastNode != null) {
        	        String title = lastNode.has("title") ? lastNode.get("title").asText() : "";
        	        String addr = lastNode.has("addr1") ? lastNode.get("addr1").asText() : "";

        	        alreadyLastIsHotel =
        	            (title.contains("호텔") || title.contains("숙소") || title.contains("리조트")) &&
        	            addr.equals(commonHotel.has("addr1") ? commonHotel.get("addr1").asText() : "");
        	    }

        	    if (!alreadyLastIsHotel) {
        	        ObjectNode newHotelNode = ((ObjectNode) commonHotel).deepCopy();
        	        newHotelNode.put("day", day);
        	        newHotelNode.put("order", maxOrder + 1);
        	        finalMap.put(day + "-" + (maxOrder + 1), newHotelNode);
        	        System.out.println("🛏️ 숙소 추가됨: day " + day + ", order " + (maxOrder + 1));
        	    } else {
        	        System.out.println("✅ 이미 마지막이 숙소: day " + day);
        	    }
        	}

        }

        // 8. 마지막 날 첫 일정에 체크아웃 추가
//        int lastDay = finalMap.values().stream()
//                              .mapToInt(n -> n.get("day").asInt())
//                              .max()
//                              .orElse(1);
//
//        String checkOutKey = lastDay + "-0";  // order 0: 맨 앞 일정
//
//        if (!finalMap.containsKey(checkOutKey)) {
//            JsonNode hotelNode = dayToHotel.get(lastDay);  // 마지막 날 숙소 한 개 가져오기
//            ObjectNode checkOutNode = mapper.createObjectNode();
//            checkOutNode.put("day", lastDay);
//            checkOutNode.put("order", 0);
//            checkOutNode.put("title", "숙소 체크아웃");
//
//            if (hotelNode != null && hotelNode.isObject()) {
//                ObjectNode hotelObj = (ObjectNode) hotelNode;
//                // 숙소에서 필요한 필드만 쏙쏙 복사
//                if (hotelObj.has("mapx")) checkOutNode.set("mapx", hotelObj.get("mapx"));
//                if (hotelObj.has("mapy")) checkOutNode.set("mapy", hotelObj.get("mapy"));
//                if (hotelObj.has("address")) checkOutNode.set("address", hotelObj.get("address"));
//                if (hotelObj.has("id")) checkOutNode.set("id", hotelObj.get("id"));
//            }
//
//            finalMap.put(checkOutKey, checkOutNode);
//            System.out.println("🧳 체크아웃 일정 추가: day " + lastDay + ", order 0");
//        }

        // 9. 정렬 후 결과 반환
        List<JsonNode> finalList = new ArrayList<>(finalMap.values());
        finalList.sort((o1, o2) -> {
            int day1 = o1.get("day").asInt();
            int day2 = o2.get("day").asInt();
            if (day1 != day2) return day1 - day2;
            return o1.get("order").asInt() - o2.get("order").asInt();
        });
        System.out.println("=== 최종 반환 장소 개수: " + finalList.size() + " ===");
        for (JsonNode place : finalList) {
            System.out.println("최종: " + place.get("day").asInt() + "-" + place.get("order").asInt()
                + " -> " + place.get("title").asText());
        }
        return finalList;
    }
    
    @Transactional(readOnly = true)
    public List<JsonNode> findAllAccommodationsFromGptOrDb(JsonNode gptArray, Set<Long> usedIds) {
        List<JsonNode> matchedHotels = new ArrayList<>();
        Set<Integer> existingHotelDays = new HashSet<>();

        for (JsonNode place : gptArray) {
            String gptTitle = place.get("title").asText().replaceAll("\\s+", "").toLowerCase();
            String regionName = normalizeRegionName(place.get("region").asText());
            String wardName = place.get("ward").asText();
            String address = place.has("address") ? place.get("address").asText() : "";

            Optional<RegionCodeEntity> regionOpt = regionCodeRepository.findRegionByNameForAI(regionName);
            if (regionOpt.isEmpty()) continue;
            RegionCodeEntity regionEntity = regionOpt.get();

            Optional<WardCodeEntity> wardOpt = wardCodeRepository.findWardByNameAndRegionForAI(wardName, regionEntity);
            if (wardOpt.isEmpty()) continue;
            WardCodeEntity wardEntity = wardOpt.get();

            List<DataEntity> candidates = dataRepository.findByRegionCodeEntityAndWardCodeEntityAndThemeIn(regionEntity, wardEntity, List.of(32));

        	if (candidates.isEmpty()) {
                System.out.println("⚠️ 해당 ward에 숙소 없음 → region 전체에서 fallback 시도: " + wardName);
                candidates = dataRepository.findByRegionCodeEntityAndThemeIn(regionEntity, List.of(32),PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "score"))).getContent();
        	}

            for (DataEntity data : candidates) {
                String dbTitle = data.getTitle().replaceAll("\\s+", "").toLowerCase();
                String dbAddr = data.getAddr1() != null ? data.getAddr1() : "";

                boolean titleMatch = gptTitle.equals(dbTitle) || dbTitle.contains(gptTitle) || gptTitle.contains(dbTitle);
                boolean addrMatch = address.contains(dbAddr) || dbAddr.contains(address);

                if (titleMatch && addrMatch && !usedIds.contains(data.getId())) {
                    usedIds.add(data.getId());
                    JsonNode hotelNode = dataEntityToJson(data, place);
                    matchedHotels.add(hotelNode);
                    if (place.has("day")) existingHotelDays.add(place.get("day").asInt());
                    break;
                }
            }
        }
        
     // 도 fallback 숙소 day별 보장
        Set<Integer> allDays = new HashSet<>();
        for (int i = 0; i < gptArray.size(); i++) {
            JsonNode node = gptArray.get(i);
            if (node.has("day")) allDays.add(node.get("day").asInt());
        }

        for (Integer day : allDays) {
            if (existingHotelDays.contains(day)) continue;

            JsonNode ref = null;
            for (int i = 0; i < gptArray.size(); i++) {
                JsonNode p = gptArray.get(i);
                if (p.has("day") && p.get("day").asInt() == day) {
                    ref = p;
                    break;
                }
            }
            if (ref == null) continue;

            String regionName = normalizeRegionName(ref.get("region").asText());
            Optional<RegionCodeEntity> regionOpt = regionCodeRepository.findRegionByNameForAI(regionName);
            if (regionOpt.isEmpty()) continue;
            RegionCodeEntity regionEntity = regionOpt.get();

            List<DataEntity> fallbackList = dataRepository.findByRegionCodeEntityAndThemeIn(
                regionEntity, List.of(32),
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "score"))
            ).getContent();

            for (DataEntity data : fallbackList) {
                if (usedIds.contains(data.getId())) continue;
                JsonNode fallbackHotel = dataEntityToJson(data, ref);
                if (fallbackHotel instanceof ObjectNode) {
                    ((ObjectNode) fallbackHotel).put("day", day);
                    ((ObjectNode) fallbackHotel).put("order", 99);
                }
                usedIds.add(data.getId());
                matchedHotels.add(fallbackHotel);
                System.out.println("🏨 도 fallback 숙소 추가됨: day " + day + " - " + data.getTitle());
                break;
            }
        }
        return matchedHotels;
    }

    // DataEntity → JsonNode 변환 예시
    public JsonNode dataEntityToJson(DataEntity data, JsonNode originalNode) {
        ObjectNode node = mapper.createObjectNode();
        node.put("id", data.getId());
        node.put("title", data.getTitle());
        node.put("region", originalNode.get("region").asText());
        node.put("ward", originalNode.get("ward").asText());
        node.put("addr1", data.getAddr1());
        node.put("addr2", data.getAddr2());
        node.put("mapx", data.getMapx());
        node.put("mapy", data.getMapy());
        node.put("day", originalNode.get("day").asInt());
        node.put("order", originalNode.get("order").asInt());
        node.put("contentId", data.getContentId());
        node.put("contentTypeId", data.getTheme());
        node.put("theme", data.getTheme());
        node.put("viewCount", data.getViewCount());
        node.put("likeCount", data.getLikeCount());
        node.put("rating", data.getRating());
        node.put("reviewCount", data.getReviewCount());
        // 추가 필드 넣어도 됨
        return node;
    } 
    
    public Map<Integer, List<ObjectNode>> groupFinalMapByDay(Map<String, ObjectNode> finalMap) {
        Map<Integer, List<ObjectNode>> grouped = new HashMap<>();
        for (ObjectNode node : finalMap.values()) {
            int day = node.get("day").asInt();
            if (!grouped.containsKey(day)) grouped.put(day, new ArrayList<>());
            grouped.get(day).add(node);
        }
        return grouped;
    }
    
    public List<ObjectNode> sortByDistance(List<ObjectNode> places) {
        if (places.size() <= 1) return places;

        List<ObjectNode> sorted = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        sorted.add(places.get(0)); // 첫 장소 고정
        visited.add(0);

        while (sorted.size() < places.size()) {
            ObjectNode last = sorted.get(sorted.size() - 1);
            double lastX = last.get("mapx").asDouble();
            double lastY = last.get("mapy").asDouble();

            int nearestIdx = -1;
            double nearestDist = Double.MAX_VALUE;

            for (int i = 0; i < places.size(); i++) {
                if (visited.contains(i)) continue;

                ObjectNode candidate = places.get(i);
                double x = candidate.get("mapx").asDouble();
                double y = candidate.get("mapy").asDouble();

                double dist = haversine(lastX, lastY, x, y);
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearestIdx = i;
                }
            }

            if (nearestIdx != -1) {
                visited.add(nearestIdx);
                sorted.add(places.get(nearestIdx));
            }
        }

        return sorted;
    }

}