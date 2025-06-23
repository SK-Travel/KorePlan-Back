package com.koreplan.controller.list;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.koreplan.dto.list.SendTravelPlanDto;
import com.koreplan.dto.list.TravelPlanDto;
import com.koreplan.service.list.TravelPlanService;


@RestController
@RequestMapping("/api/my-plan")
public class TravelListRestController {
	
	@Autowired
	private TravelPlanService travelPlanService;
	
//	@Autowired
//	private UserService userService;
	
    // AI로 리스트 추가
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addToMyPlan(
            @RequestBody TravelPlanDto travelPlanDto,
            @RequestHeader("userId") Integer userId) {

        travelPlanService.addPlan(travelPlanDto);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "리스트 저장 성공");
        return ResponseEntity.ok(result);
    }
    
    // 나만의 리스트 추가 
    @PostMapping("/add-my-plan")
    public Map<String, Object> addMyOwnPlan(@RequestHeader("userId") Integer userId,
    		@RequestBody TravelPlanDto travelPlanDto) {
    	
    	travelPlanService.addPlan(travelPlanDto);
    	
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

    // 리스트 삭제
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> deletePlan(@PathVariable Long id) {
        travelPlanService.deleteTravelPlanByPlanId(id);

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "삭제 성공");
        return ResponseEntity.ok(response);
    }
}
