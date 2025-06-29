package com.koreplan.controller.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.koreplan.data.service.SearchDataService;
import com.koreplan.dto.festival.FestivalResponseDto;
import com.koreplan.dto.search.DataResponseDto;
import com.koreplan.service.search.FilterDataService.SortType;
import com.koreplan.service.search.MapSearchService;

@RestController
@RequestMapping("/api/map-search")
public class SearchDataController {
	@Autowired
	private MapSearchService mapSearchService;
	@Autowired
	private SearchDataService searchDataService;
	@GetMapping("/nearby")
	public ResponseEntity<?> getNearbyPlacesByTheme(
	        @RequestParam double lat,
	        @RequestParam double lng,
	        @RequestParam int theme,
	        @RequestParam(defaultValue = "5000") int radius,
	        @RequestParam(defaultValue = "0") int page,
	        @RequestParam(defaultValue = "10") int size) {
	    
	    try {
	        Pageable pageable = PageRequest.of(page, size);
	        
	        if (theme == 15) {
	            // 축제 검색
	            Page<FestivalResponseDto> festivals = mapSearchService.getNearbyFestivalsByTheme(
	                    lat, lng, radius, pageable);
	            
	            // 축제 응답 구조
	            Map<String, Object> response = new HashMap<>();
	            response.put("code", 200);
	            response.put("message", "축제 검색 성공");
	            response.put("result", festivals.getContent());
	            response.put("pagination", Map.of(
	                    "currentPage", festivals.getNumber(),
	                    "totalPages", festivals.getTotalPages(),
	                    "totalElements", festivals.getTotalElements(),
	                    "pageSize", festivals.getSize(),
	                    "hasNext", festivals.hasNext(),
	                    "hasPrevious", festivals.hasPrevious()
	            ));
	            
	            return ResponseEntity.ok(response);
	            
	        } else {
	            // 일반 장소 검색
	            Page<DataResponseDto> places = mapSearchService.getNearbyPlacesByTheme(
	                    lat, lng, theme, radius, pageable);
	            
	            // 일반 장소 응답 구조
	            Map<String, Object> response = new HashMap<>();
	            response.put("code", 200);
	            response.put("message", "장소 검색 성공");
	            response.put("result", places.getContent());
	            response.put("pagination", Map.of(
	                    "currentPage", places.getNumber(),
	                    "totalPages", places.getTotalPages(),
	                    "totalElements", places.getTotalElements(),
	                    "pageSize", places.getSize(),
	                    "hasNext", places.hasNext(),
	                    "hasPrevious", places.hasPrevious()
	            ));
	            
	            return ResponseEntity.ok(response);
	        }
	        
	    } catch (Exception e) {
	        Map<String, Object> errorResponse = new HashMap<>();
	        errorResponse.put("code", 500);
	        errorResponse.put("message", "검색 실패: " + e.getMessage());
	        errorResponse.put("result", new ArrayList<>());
	        
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body(errorResponse);
	    }
	}

	@GetMapping("/keyword")
	public ResponseEntity<Map<String, Object>> searchByKeyword(@RequestParam("keyword") String keyword,
			@RequestParam(value = "sort", defaultValue = "SCORE") String sortType,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size) {

		Map<String, Object> response = new HashMap<>();

		try {
			// 키워드 유효성 검사
			if (keyword == null || keyword.trim().isEmpty()) {
				response.put("code", 400);
				response.put("message", "검색 키워드가 필요합니다.");
				return ResponseEntity.badRequest().body(response);
			}

			// SortType enum 변환
			SortType sort;
			try {
				sort = SortType.valueOf(sortType.toUpperCase());
			} catch (IllegalArgumentException e) {
				sort = SortType.SCORE; // 기본값
			}

			// Pageable 객체 생성
			Pageable pageable = PageRequest.of(page, size);

			// 서비스에서 DTO 변환까지 처리
			Page<DataResponseDto> searchPage = searchDataService.searchByKeywordWithPagingDto(keyword.trim(), sort,
					pageable);

			response.put("code", 200);
			response.put("message", "키워드 검색 성공");
			response.put("result", searchPage.getContent());

			// 페이징 정보
			response.put("pagination",
					Map.of("currentPage", page, "totalPages", searchPage.getTotalPages(), "totalElements",
							searchPage.getTotalElements(), "size", size, "hasNext", searchPage.hasNext(), "hasPrevious",
							searchPage.hasPrevious(), "isFirst", searchPage.isFirst(), "isLast", searchPage.isLast()));

			// 검색 조건 정보
			response.put("searchInfo", Map.of("keyword", keyword.trim(), "sortType", sort.toString()));

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			response.put("code", 500);
			response.put("message", "키워드 검색 실패: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
}
