package com.koreplan.controller.list;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.koreplan.dto.list.AIPlanDto;
import com.koreplan.dto.list.DataSearchDto;
import com.koreplan.dto.list.ReceiveTravelPlanDto;
import com.koreplan.dto.list.SendTravelPlanDto;
import com.koreplan.service.list.TravelPlanService;


@RestController
@RequestMapping("/api/my-plan")
public class TravelListRestController {
	
	@Autowired
	private TravelPlanService travelPlanService;

	// keyword로 DataId검색 로직하고 저장하는 것. TravelPlanService -> SearchDataService -> keyword로 dataId, title, RegionName 받아서 보내기
	@GetMapping("/search")
	public ResponseEntity<Map<String, Object>> searchData(
	        @RequestParam("keyword") String keyword) {

	    Map<String, Object> response = new HashMap<>();

	    if (keyword == null || keyword.trim().isEmpty()) {
	        response.put("code", 400);
	        response.put("message", "검색어가 비어 있습니다.");
	        return ResponseEntity.badRequest().body(response);
	    }

	    List<DataSearchDto> searchResults = travelPlanService.searchDataByKeyword(keyword);

	    response.put("code", 200);
	    response.put("message", "검색 성공");
	    response.put("result", searchResults);

	    return ResponseEntity.ok(response);
	}

	
    // AI로 리스트 추가
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addToMyPlan(
            @RequestBody AIPlanDto travelPlanDto,
            @RequestHeader("userId") Integer userId) {

        travelPlanService.addAiPlan(travelPlanDto);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "리스트 저장 성공");
        return ResponseEntity.ok(result);
    }
    
    // 나만의 리스트 추가 
    @PostMapping("/add-my-plan")
    public Map<String, Object> addMyOwnPlan(@RequestHeader("userId") Integer userId,
    		@RequestBody ReceiveTravelPlanDto travelPlanDto) {
    
    	travelPlanService.addOwnPlan(travelPlanDto);
    	
    	Map<String, Object> result = new HashMap<>();
    	if (userId != null) {
    		result.put("code", 200);
    		result.put("success", "리스트 저장 성공");
    	} else {
    		result.put("code" , 500);
    		result.put("error_message" , "리스트 저장 실패");
    	}
    	
    	return result;
    }
    
    

    // 리스트 조회
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getMyPlans(@RequestHeader("userId") Integer userId) {
    	List<SendTravelPlanDto> sendTravelDto = travelPlanService.getPlanListDtoByUserId(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "조회 성공");
        response.put("result", sendTravelDto);
        return ResponseEntity.ok(response);
    }
    
    // 사용자의 특정 계획 조회 (상세/수정 페이지용)
    @GetMapping("/detail/{planId}")
    public ResponseEntity<Map<String, Object>> getPlanDetail(
            @PathVariable Long planId,
            @RequestHeader("userId") Integer userId) {
        
        SendTravelPlanDto planDetail = travelPlanService.getPlanDto(userId, planId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "상세 조회 성공");
        response.put("result", planDetail);
        
        return ResponseEntity.ok(response);
    }
    
    // 수정 시 일정 하나 삭제
    @DeleteMapping("/delete/{planId}/{dataId}")
    public ResponseEntity<?> deleteLocationFromPlan(
            @PathVariable Long planId,
            @PathVariable Long dataId,
            @RequestParam int day,
            @RequestParam int order,
            @RequestHeader("userId") Long userId) {

        boolean success = travelPlanService.deleteSingleLocationFromPlan(planId, dataId, day, order, userId);
        if (success) {
            return ResponseEntity.ok("삭제 성공");
        } else {
            return ResponseEntity.badRequest().body("삭제 실패: 조건 불일치 또는 권한 없음");
        }
    }
    
    // 리스트 저체 수정
    @PutMapping("/update/{planId}")
    public ResponseEntity<?> updateTravelPlan(
        @PathVariable Long planId,
        @RequestBody ReceiveTravelPlanDto travelPlanDto,
        @RequestHeader("userId") int userIdHeader) {
        // 권한 체크: URL userId와 헤더 userId 일치 확인 (필요 시)
        if (userIdHeader != travelPlanDto.getUserId()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("권한이 없습니다.");
        }
        travelPlanDto.setId(planId);

        boolean updated = travelPlanService.updatePlan(travelPlanDto);

        if (updated) {
            return ResponseEntity.ok(Map.of("code", 200, "message", "여행 계획 수정 성공"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("code", 400, "message", "수정 실패"));
        }
        
    }
    
    
    
    // 리스트 전체 삭제
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> deletePlan(@PathVariable Long id) {
        travelPlanService.deleteTravelPlanByPlanId(id);

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "삭제 성공");
        return ResponseEntity.ok(response);
    }
}
