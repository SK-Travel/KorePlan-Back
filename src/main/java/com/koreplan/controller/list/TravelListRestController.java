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

import com.koreplan.entity.list.TravelPlanEntity;
import com.koreplan.service.list.TravelPlanService;
import com.koreplan.user.service.UserService;

@RestController
@RequestMapping("/api/my-plan")
public class TravelListRestController {
	
	@Autowired
	private TravelPlanService travelPlanService;
	@Autowired
	private UserService userService;
	
    // 리스트 추가
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addToMyPlan(
            @RequestBody TravelPlanEntity travelPlan,
            @RequestHeader("userId") Integer userId) {

        travelPlan.setUserId(userId);
        TravelPlanEntity savedPlan = travelPlanService.addPlan(travelPlan);

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "리스트 저장 성공");
        response.put("data", savedPlan);
        return ResponseEntity.ok(response);
    }

    // 리스트 조회
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getMyPlans(@RequestHeader("userId") Integer userId) {
        List<TravelPlanEntity> plans = travelPlanService.getPlanListByUserId(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "조회 성공");
        response.put("data", plans);
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
