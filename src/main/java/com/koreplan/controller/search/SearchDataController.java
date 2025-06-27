package com.koreplan.controller.search;

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
	public ResponseEntity<Map<String, Object>> getNearbyPlacesByTheme(@RequestParam("lat") double latitude,
			@RequestParam("lng") double longitude, @RequestParam("theme") int theme,
			@RequestParam(value = "radius", defaultValue = "5000") int radius, // 반경 5km (미터)
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size) {

		Map<String, Object> response = new HashMap<>();

		try {
			// Pageable 객체 생성
			Pageable pageable = PageRequest.of(page, size);

			Page<DataResponseDto> nearbyPlacesPage = mapSearchService.getNearbyPlacesByTheme(latitude, longitude, theme,
					radius, pageable);

			response.put("code", 200);
			response.put("message", "주변 장소 조회 성공");
			response.put("result", nearbyPlacesPage.getContent());

			// 페이징 정보
			response.put("pagination",
					Map.of("currentPage", page, "totalPages", nearbyPlacesPage.getTotalPages(), "totalElements",
							nearbyPlacesPage.getTotalElements(), "size", size, "hasNext", nearbyPlacesPage.hasNext(),
							"hasPrevious", nearbyPlacesPage.hasPrevious(), "isFirst", nearbyPlacesPage.isFirst(),
							"isLast", nearbyPlacesPage.isLast()));

			// 검색 조건 정보
			response.put("searchInfo", Map.of("lat", latitude, "lng", longitude, "theme", theme, "radius", radius));

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			response.put("code", 500);
			response.put("message", "주변 장소 조회 실패: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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
