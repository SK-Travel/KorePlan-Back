package com.koreplan.service.festival;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koreplan.area.entity.RegionCodeEntity;
import com.koreplan.area.entity.WardCodeEntity;
import com.koreplan.area.repository.RegionCodeRepository;
import com.koreplan.area.repository.WardCodeRepository;
import com.koreplan.dto.ApiResponseDto;
import com.koreplan.dto.festival.FestivalCommonDto;
import com.koreplan.dto.festival.FestivalContentIdDto;
import com.koreplan.dto.festival.FestivalTermDto;
import com.koreplan.entity.festival.FestivalEntity;
import com.koreplan.repository.festival.FestivalRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class SaveFestivalService {
	@Value("${publicDataKey}")
	private String serviceKey1;  // 첫 번째 서비스키
	
	@Value("${publicDataKey2}")  // 두 번째 서비스키 추가
	private String serviceKey2;

	private final String API_BASE_URL = "https://apis.data.go.kr/B551011/KorService2/";
	private final String First_API_URL = "areaBasedList2";
	private final String Second_API_URL = "detailIntro2";
	private final String Third_API_URL = "detailCommon2";
	
	private final ObjectMapper objectMapper;
	
	private final RegionCodeRepository regionCodeRepository;
	private final WardCodeRepository wardCodeRepository;
    private final FestivalRepository festivalRepository;
    
    /**
     * 서비스키를 교대로 반환
     */
    private String getCurrentServiceKey(int index) {
        String key = (index % 2 == 0) ? serviceKey1 : serviceKey2;
        log.debug("사용할 서비스키 (인덱스 {}): {}", index, key.substring(0, 10) + "...");
        return key;
    }
    
	// 제일 먼저 축제 타입인 모든 데이터 갖고오기.
	public ResponseEntity<ApiResponseDto<FestivalContentIdDto>> requestFirst() throws Exception {
		int rows = 10000;
		String fullUrl = API_BASE_URL + First_API_URL + "?serviceKey=" + serviceKey1 + "&numOfRows=" + rows
				+ "&MobileOS=WEB&MobileApp=Koreplan&_type=json&contentTypeId=15";
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
		
		// 디버깅: 실제 API 응답 로그 출력 (다시 500자로 제한)
		log.info("첫 번째 API 응답 샘플: {}", responseBody.length() > 500 ? responseBody.substring(0, 500) + "..." : responseBody);
		
		try {
			// 문자열을 ResponseDto 객체로 변환
			ApiResponseDto responseDto = objectMapper.readValue(responseBody, objectMapper.getTypeFactory()
					.constructParametricType(ApiResponseDto.class, FestivalContentIdDto.class));
			
			// 디버깅: 파싱된 첫 번째 데이터 상세 확인
			if (responseDto != null && responseDto.getResponse() != null && 
			    responseDto.getResponse().getBody() != null && 
			    responseDto.getResponse().getBody().getItems() != null &&
			    !responseDto.getResponse().getBody().getItems().getItem().isEmpty()) {
			    FestivalContentIdDto firstItem = (FestivalContentIdDto) responseDto.getResponse().getBody().getItems().getItem().get(0);
			    log.info("첫 번째 축제 상세: {}", firstItem);
			}
			
			return ResponseEntity.ok(responseDto);
		} catch (Exception e) {
			log.error("JSON 변환 중 오류 발생: {}", responseBody, e);
			throw new RuntimeException("JSON 변환 중 오류 발생", e);
		}
	}

	// requestFirst로 갖고온 Dto의 contentID로 축제의 기간정보 불러오기. (서비스키 매개변수 추가)
	public ResponseEntity<ApiResponseDto<FestivalTermDto>> requestSecond(FestivalContentIdDto dto, String serviceKey) throws Exception {
	    String contentID = dto.getContentId();
	    log.debug("requestSecond 호출 - 입력 매개변수: {}, 서비스키: {}", dto, serviceKey.substring(0,10) + "...");
	    log.debug("requestSecond - contentID: {}", contentID);
	    
	    String fullUrl = API_BASE_URL + Second_API_URL + "?serviceKey=" + serviceKey
	            + "&MobileOS=WEB&MobileApp=Koreplan&_type=json&contentId=" + contentID + "&contentTypeId=15";
	    log.debug("requestSecond - 호출 URL: {}", fullUrl);
	    
	    // API 호출 제한을 위한 딜레이 추가
	    Thread.sleep(50); // 0.05초 대기
	    
	    URL url = new URL(fullUrl);
	    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	    conn.setRequestMethod("GET");
	    conn.setRequestProperty("Content-type", "application/json");
	    conn.setConnectTimeout(10000); // 10초로 단축
	    conn.setReadTimeout(10000);    // 10초로 단축
	    
	    BufferedReader rd;
	    if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
	        rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	    } else {
	        rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
	        log.error("API 호출 실패: {}, contentId: {}", conn.getResponseMessage(), contentID);
	        return null;
	    }

	    StringBuilder sb = new StringBuilder();
	    String line;
	    while ((line = rd.readLine()) != null) {
	        sb.append(line);
	    }
	    rd.close();
	    conn.disconnect();
	    String responseBody = sb.toString();
	    log.debug("requestSecond - API 응답: {}", responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody);
	    
	    try {
	        // XML 에러 응답 체크
	        if (responseBody.trim().startsWith("<")) {
	            log.error("API에서 XML 에러 응답 수신. contentId: {}, response: {}", contentID, responseBody);
	            
	            // 호출 제한 에러인 경우 더 긴 대기
	            if (responseBody.contains("LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR")) {
	                log.warn("API 호출 제한 초과! 10초 대기 후 재시도...");
	                Thread.sleep(10000);
	            }
	            return null;
	        }
	        
	        // 빈 items 체크 및 처리
	        if (responseBody.contains("\"items\": \"\"") || responseBody.contains("\"totalCount\":0")) {
	            log.warn("API 응답에 데이터가 없습니다. contentId: {}", contentID);
	            return null; // null 반환으로 처리
	        }
	        
	        // 문자열을 ResponseDto 객체로 변환
	        ApiResponseDto responseDto = objectMapper.readValue(responseBody, objectMapper.getTypeFactory()
	                .constructParametricType(ApiResponseDto.class, FestivalTermDto.class));
	        
	        // 디버깅: 파싱된 FestivalTermDto 확인
	        if (responseDto != null && responseDto.getResponse() != null && 
	            responseDto.getResponse().getBody() != null && 
	            responseDto.getResponse().getBody().getItems() != null &&
	            !responseDto.getResponse().getBody().getItems().getItem().isEmpty()) {
	            FestivalTermDto termDto = (FestivalTermDto) responseDto.getResponse().getBody().getItems().getItem().get(0);
	            log.debug("requestSecond - 파싱된 TermDto: {}", termDto);
	        }
	        
	        return ResponseEntity.ok(responseDto);
	    } catch (Exception e) {
	        log.error("JSON 변환 중 오류 발생. contentId: {}, response: {}", contentID, responseBody, e);
	        return null; // 예외 발생시 null 반환
	    }
	}

	public ResponseEntity<ApiResponseDto<FestivalCommonDto>> requestThird(FestivalContentIdDto dto, String serviceKey) throws Exception{
	    String contentID = dto.getContentId();
	    log.debug("requestThird 호출 - contentID: {}, 서비스키: {}", contentID, serviceKey.substring(0,10) + "...");
	    
	    String fullUrl = API_BASE_URL + Third_API_URL + "?serviceKey=" + serviceKey
	            + "&MobileOS=WEB&MobileApp=Koreplan&_type=json&contentId=" + contentID;
	    
	    // API 호출 제한을 위한 딜레이 추가
	    Thread.sleep(30); // 0.03초 대기
	    
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
	        log.error("API 호출 실패: {}, contentId: {}", conn.getResponseMessage(), contentID);
	        return null;
	    }

	    StringBuilder sb = new StringBuilder();
	    String line;
	    while ((line = rd.readLine()) != null) {
	        sb.append(line);
	    }
	    rd.close();
	    conn.disconnect();
	    String responseBody = sb.toString();
	    log.debug("requestThird - API 응답: {}", responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody);
	    
	    try {
	        // XML 에러 응답 체크
	        if (responseBody.trim().startsWith("<")) {
	            log.error("API에서 XML 에러 응답 수신. contentId: {}, response: {}", contentID, responseBody);
	            
	            // 호출 제한 에러인 경우 더 긴 대기
	            if (responseBody.contains("LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR")) {
	                log.warn("API 호출 제한 초과! 10초 대기 후 재시도...");
	                Thread.sleep(10000);
	            }
	            return null;
	        }
	        
	        // 빈 items 체크 및 처리
	        if (responseBody.contains("\"items\": \"\"") || responseBody.contains("\"totalCount\":0")) {
	            log.warn("API 응답에 데이터가 없습니다. contentId: {}", contentID);
	            return null;
	        }
	        
	        // 문자열을 ResponseDto 객체로 변환
	        ApiResponseDto responseDto = objectMapper.readValue(responseBody, objectMapper.getTypeFactory()
	                .constructParametricType(ApiResponseDto.class, FestivalCommonDto.class));
	        return ResponseEntity.ok(responseDto);
	    } catch (Exception e) {
	        log.error("JSON 변환 중 오류 발생. contentId: {}, response: {}", contentID, responseBody, e);
	        return null;
	    }
	}

	// 재시도 로직이 포함된 메서드들 (서비스키 매개변수 추가)
	private ResponseEntity<ApiResponseDto<FestivalTermDto>> requestSecondWithRetry(FestivalContentIdDto dto, String serviceKey) throws Exception {
	    int maxRetries = 3;
	    for (int i = 0; i < maxRetries; i++) {
	        try {
	            ResponseEntity<ApiResponseDto<FestivalTermDto>> result = requestSecond(dto, serviceKey);
	            if (result != null) {
	                return result;
	            }
	            if (i < maxRetries - 1) {
	                Thread.sleep(5000); // 5초 대기 후 재시도
	            }
	        } catch (Exception e) {
	            if (i == maxRetries - 1) {
	                log.error("재시도 실패: contentId={}, 키={}", dto.getContentId(), serviceKey.substring(0,10) + "...", e);
	                return null;
	            }
	            Thread.sleep(5000);
	        }
	    }
	    return null;
	}

	private ResponseEntity<ApiResponseDto<FestivalCommonDto>> requestThirdWithRetry(FestivalContentIdDto dto, String serviceKey) throws Exception {
	    int maxRetries = 3;
	    for (int i = 0; i < maxRetries; i++) {
	        try {
	            ResponseEntity<ApiResponseDto<FestivalCommonDto>> result = requestThird(dto, serviceKey);
	            if (result != null) {
	                return result;
	            }
	            if (i < maxRetries - 1) {
	                Thread.sleep(5000);
	            }
	        } catch (Exception e) {
	            if (i == maxRetries - 1) {
	                log.error("재시도 실패: contentId={}, 키={}", dto.getContentId(), serviceKey.substring(0,10) + "...", e);
	                return null;
	            }
	            Thread.sleep(5000);
	        }
	    }
	    return null;
	}

	// saveFestival 메서드 - 서비스키 교대 사용 (전체 데이터 처리)
	public void saveFestival(ApiResponseDto<FestivalContentIdDto> dto) throws Exception {
	    List<FestivalContentIdDto> allFestival = dto.getResponse().getBody().getItems().getItem();
	    List<FestivalEntity> entities = new ArrayList<>();
	    
	    int successCount = 0;
	    int errorCount = 0;
	    int processedCount = 0;
	    int totalCount = allFestival.size();
	    
	    log.info("전체 축제 데이터 처리 시작: 총 {}개 (서비스키 2개 교대 사용)", totalCount);
	    
	    for(int i = 0; i < allFestival.size(); i++) {
	        FestivalContentIdDto festival = allFestival.get(i);
	        String currentKey = getCurrentServiceKey(i); // 교대로 키 사용
	        
	        processedCount++;
	        
	        try {
	            // 중복 체크 (주석 처리됨)
	            // if (festivalRepository.existsByContentId(festival.getContentId())) {
	            //     log.info("이미 존재하는 축제 (스킵): contentId={}", festival.getContentId());
	            //     continue;
	            // }
	            
	            // 진행률 로그 (100개마다 출력)
	            if (processedCount % 100 == 0) {
	                log.info("처리 진행률: {}/{} ({:.1f}%) - 현재 contentId: {}, 사용키: {}", 
	                    processedCount, totalCount, (processedCount * 100.0 / totalCount), 
	                    festival.getContentId(), currentKey.substring(0,10) + "...");
	            }
	            
	            // 두번째 API 호출 (재시도 로직 포함)
	            ResponseEntity<ApiResponseDto<FestivalTermDto>> termDto = requestSecondWithRetry(festival, currentKey);
	            
	            // null 체크 추가
	            if (termDto == null || termDto.getBody() == null || 
	                termDto.getBody().getResponse().getBody().getItems() == null ||
	                termDto.getBody().getResponse().getBody().getItems().getItem().isEmpty()) {
	                log.warn("축제 기간 정보가 없습니다. contentId: {}", festival.getContentId());
	                errorCount++;
	                continue;
	            }
	            
	            FestivalTermDto check = termDto.getBody().getResponse().getBody().getItems().getItem().getFirst();
	            
	            // 축제 종료 날짜 체크
	            if(isExpiredFestival(check.getEventEndDate())) {
	                log.debug("만료된 축제 (스킵): contentId={}, 종료일={}", 
	                    festival.getContentId(), check.getEventEndDate());
	                continue;
	            }
	            
	            // 세번째 API 호출 (재시도 로직 포함)
	            ResponseEntity<ApiResponseDto<FestivalCommonDto>> commonDto = requestThirdWithRetry(festival, currentKey);
	            
	            // null 체크 추가
	            if (commonDto == null || commonDto.getBody() == null || 
	                commonDto.getBody().getResponse().getBody().getItems() == null ||
	                commonDto.getBody().getResponse().getBody().getItems().getItem().isEmpty()) {
	                log.warn("축제 공통 정보가 없습니다. contentId: {}", festival.getContentId());
	                errorCount++;
	                continue;
	            }
	            
	            FestivalCommonDto last = commonDto.getBody().getResponse().getBody().getItems().getItem().getFirst();
	            
	            // Entity 생성 및 설정
	            FestivalEntity entity = createFestivalEntity(check, last);
	            entities.add(entity);
	            successCount++;
	            
	            // 성공 로그는 debug로 변경 (너무 많아지지 않도록)
	            log.debug("축제 저장 준비: {} (키: {})", last.getTitle(), currentKey.substring(0,10) + "...");
	            
	        } catch (Exception e) {
	            log.error("축제 처리 중 오류: contentId={}, 키={}", 
	                festival.getContentId(), currentKey.substring(0,10) + "...", e);
	            errorCount++;
	        }
	    }
	    
	    // 마지막에 모든 데이터 한번에 저장
	    if (!entities.isEmpty()) {
	        log.info("DB 저장 시작: {}개 축제 데이터", entities.size());
	        festivalRepository.saveAll(entities);
	        log.info("DB 저장 완료: {}개 축제 데이터", entities.size());
	    }
	    
	    log.info("전체 데이터 처리 완료! 처리: {}, 성공: {}, 실패: {}, 최종 저장: {}", 
	        processedCount, successCount, errorCount, entities.size());
	}
	
	// Entity 생성 로직 분리
	private FestivalEntity createFestivalEntity(FestivalTermDto check, FestivalCommonDto last) {
	    FestivalEntity entity = new FestivalEntity();
	    entity.setContentId(check.getContentId());
	    entity.setContentTypeId(15);
	    
	    LocalDate start = parseDate(check.getEventStartDate());
	    LocalDate end = parseDate(check.getEventEndDate());
	    entity.setEventStartDate(start);
	    entity.setEventEndDate(end);
	    
	    entity.setAddr1(last.getAddr1());
	    entity.setAddr2(last.getAddr2());
	    entity.setFirstimage(last.getFirstimage());
	    entity.setFirstimage2(last.getFirstimage2());
	    entity.setMapx(last.getMapx());
	    entity.setMapy(last.getMapy());
	    entity.setC1Code(last.getLclsSystm1());
	    entity.setC2Code(last.getLclsSystm2());
	    entity.setC3Code(last.getLclsSystm3());
	    entity.setOverview(last.getOverview());
	    entity.setTitle(last.getTitle());
	    entity.setViewCount(0);
	    
	    // 지역 코드 매핑
	    setRegionInfo(entity, last);
	    
	    return entity;
	}

	private void setRegionInfo(FestivalEntity entity, FestivalCommonDto last) {
	    String regioncodeStr = last.getLDongRegnCd();
	    String wardcodeStr = last.getLDongSignguCd();
	    
	    RegionCodeEntity regionEntity = null;
	    WardCodeEntity wardEntity = null;

	    if (regioncodeStr != null && !regioncodeStr.trim().isEmpty()) {
	        try {
	            Long regioncode = Long.valueOf(regioncodeStr);
	            regionEntity = regionCodeRepository.findByRegioncode(regioncode).orElse(null);

	            if (wardcodeStr != null && !wardcodeStr.trim().isEmpty()) {
	                Long wardcode = Long.valueOf(wardcodeStr);
	                wardEntity = wardCodeRepository.findByWardcodeAndRegionCodeEntity_Regioncode(wardcode, regioncode).orElse(null);
	            }
	        } catch (NumberFormatException e) {
	            log.warn("지역 코드 파싱 오류: region='{}', ward='{}'", regioncodeStr, wardcodeStr);
	        }
	    }

	    entity.setRegionCodeEntity(regionEntity);
	    entity.setWardCodeEntity(wardEntity);
	}
	
	// @PostConstruct  // 주석 처리 - 이미 데이터가 들어있음
	public void init() {
	    try {
	        log.info("전체 축제 데이터 초기화를 시작합니다... (서비스키 2개 교대 사용)");
	        
	        // 첫 번째 API 호출 (모든 축제 목록)
	        ResponseEntity<ApiResponseDto<FestivalContentIdDto>> response = requestFirst();
	        ApiResponseDto<FestivalContentIdDto> dto = response.getBody();

	        if (dto == null) {
	            log.warn("API 응답이 null입니다.");
	            return;
	        }

	        List<FestivalContentIdDto> items = dto.getResponse().getBody().getItems().getItem();
	        
	        if (items == null || items.isEmpty()) {
	            log.warn("축제 데이터가 없습니다.");
	            return;
	        }
	        
	        log.info("총 {}개 축제 데이터를 전체 처리합니다. (서비스키 2개 교대 사용)", items.size());

	        // 축제 데이터 저장
	        saveFestival(dto);
	        
	        log.info("전체 축제 데이터 초기화가 완료되었습니다.");

	    } catch (Exception e) {
	        log.error("축제 데이터 초기화 중 오류 발생", e);
	    }
	}
	
	// 배치 처리를 위한 새로운 메서드 (주석 처리됨)
	/*
	public void initWithBatch(int startIndex, int batchSize) throws Exception {
	    log.info("배치 처리 시작: startIndex={}, batchSize={} (서비스키 2개 교대 사용)", startIndex, batchSize);
	    
	    // 첫 번째 API 호출 (모든 축제 목록)
	    ResponseEntity<ApiResponseDto<FestivalContentIdDto>> response = requestFirst();
	    ApiResponseDto<FestivalContentIdDto> dto = response.getBody();

	    if (dto == null) {
	        throw new RuntimeException("API 응답이 null입니다.");
	    }

	    List<FestivalContentIdDto> allItems = dto.getResponse().getBody().getItems().getItem();
	    
	    if (allItems == null || allItems.isEmpty()) {
	        throw new RuntimeException("축제 데이터가 없습니다.");
	    }
	    
	    // 배치 범위 계산
	    int endIndex = Math.min(startIndex + batchSize, allItems.size());
	    List<FestivalContentIdDto> batchItems = allItems.subList(startIndex, endIndex);
	    
	    log.info("전체 {}개 중 {}~{} 범위의 {}개 데이터를 처리합니다.", 
	             allItems.size(), startIndex, endIndex-1, batchItems.size());
	    
	    // 배치용 DTO 생성
	    ApiResponseDto<FestivalContentIdDto> batchDto = new ApiResponseDto<>();
	    ApiResponseDto.Response<FestivalContentIdDto> batchResponse = new ApiResponseDto.Response<>();
	    ApiResponseDto.Body<FestivalContentIdDto> batchBody = new ApiResponseDto.Body<>();
	    ApiResponseDto.Items<FestivalContentIdDto> batchItemsDto = new ApiResponseDto.Items<>();
	    
	    batchItemsDto.setItem(batchItems);
	    batchBody.setItems(batchItemsDto);
	    batchResponse.setBody(batchBody);
	    batchDto.setResponse(batchResponse);
	    
	    // 배치 데이터 저장
	    saveFestival(batchDto);
	    
	    log.info("배치 처리 완료: {}개 처리됨", batchItems.size());
	}
	*/
	
	//기간 검사 메서드
	private boolean isExpiredFestival(String eventEndDate) {
		if (eventEndDate == null || eventEndDate.isEmpty()) {
			return true; // 날짜 정보가 없으면 만료된 것으로 처리
		}
		LocalDate endDate = parseDate(eventEndDate);
		return endDate != null && endDate.isBefore(LocalDate.now());
	}
	
	//형변환 메서드
	private LocalDate parseDate(String dateStr) {
		if (dateStr == null || dateStr.isEmpty()) {
			return null;
		}
		return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
	}
}