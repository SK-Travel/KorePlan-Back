package com.koreplan.controller.search;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/region-data")
@RequiredArgsConstructor
@Slf4j
//@CrossOrigin(origins = "*") // 개발용 - 운영시에는 특정 도메인으로 제한
public class SearchRestController {

    //private final RegionFilterDataService regionFilterDataService;

    /**
     * 간단한 상태 확인용 API
     * GET /api/region-data/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Region Data API is running!");
    }
}