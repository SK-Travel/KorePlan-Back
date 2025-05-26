package com.koreplan.data.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koreplan.area.entity.RegionCodeEntity;
import com.koreplan.area.entity.WardCodeEntity;
import com.koreplan.area.repository.RegionCodeRepository;
import com.koreplan.area.repository.WardCodeRepository;
import com.koreplan.category.entity.CategoryEntity;
import com.koreplan.category.repository.CategoryRepository;
import com.koreplan.data.dto.DataDto;
import com.koreplan.data.dto.ResponseDto;
import com.koreplan.data.entity.DataEntity;
import com.koreplan.data.repository.DataRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaveDataService {

	private final DataRepository dataRepository;
	private final ObjectMapper objectMapper;
	
	private final RegionCodeRepository regionCodeRepository;
	private final WardCodeRepository wardCodeRepository;
	private final CategoryRepository categoryRepository;
	
	
	
	@Value("${publicDataKey}")
	private String key;

	private String apiUrl = "https://apis.data.go.kr/B551011/KorService2/areaBasedList2";

	public ResponseEntity<ResponseDto> requestData() throws Exception {

		int rows = 100000;
		String fullUrl = apiUrl + "?serviceKey=" + key + "&numOfRows=" + rows
				+ "&MobileOS=WEB&MobileApp=Koreplan&_type=json";
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
			// 문자열을 ResponseDto 객체로 변환
			ResponseDto responseDto = objectMapper.readValue(responseBody, ResponseDto.class);
			return ResponseEntity.ok(responseDto);
		} catch (Exception e) {
			log.error("JSON 변환 중 오류 발생: {}", responseBody, e);
			throw new RuntimeException("JSON 변환 중 오류 발생", e);
		}
	}


//	@PostConstruct
//	public void init() {
//		try {
//			ResponseEntity<ResponseDto> response = requestData();
//			ResponseDto dto = response.getBody();
//
//			if (dto == null) {
//				log.warn("API 응답이 null입니다.");
//				return;
//			}
//
//			List<DataDto> items = dto.getResponse().getBody().getItems().getItem();
//			
//			saveData(dto);
//
//
//		} catch (Exception e) {
//			log.error("지역 코드 초기화 중 오류 발생", e);
//		}
//	}

	public void saveData(ResponseDto dto) {
		
	    List<DataDto> items = dto.getResponse()
	                              .getBody()
	                              .getItems()
	                              .getItem();

	    List<DataEntity> entities = new ArrayList<>();
	    
	    for (DataDto item : items) {

			DataEntity entity = new DataEntity();
			entity.setContentId(item.getContentid());
			entity.setAddr1(item.getAddr1());
			entity.setAddr2(item.getAddr2());
			entity.setMapx(item.getMapx());
			entity.setMapy(item.getMapy());
			entity.setTitle(item.getTitle());
			entity.setC1Code(item.getLclsSystm1());
			entity.setC2Code(item.getLclsSystm2());
			entity.setC3Code(item.getLclsSystm3());
			entity.setFirstimage(item.getFirstimage());
			entity.setFirstimage2(item.getFirstimage2());
			String tel = item.getTel();
			if (tel != null && tel.length() > 50) {
				tel = tel.substring(0, 50);
			}
			entity.setTel(tel);
	       
	        String regioncodeStr = item.getLDongRegnCd();
	        String wardcodeStr = item.getLDongSignguCd();
	        
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
	                log.warn("지역 코드 또는 시군구 코드 파싱 오류: region='{}', ward='{}'", regioncodeStr, wardcodeStr);
	            }
	        } else {
	            log.warn("빈 지역코드 또는 잘못된 값: region='{}', ward='{}'", regioncodeStr, wardcodeStr);
	        }

	        entity.setRegionCodeEntity(regionEntity);
	        entity.setWardCodeEntity(wardEntity);


	        entities.add(entity);
	        
	        log.info("DataEntity created: {}", entity);
	    }
	    dataRepository.saveAll(entities);
	    
	}
}
