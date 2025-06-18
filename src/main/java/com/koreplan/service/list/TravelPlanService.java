package com.koreplan.service.list;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.koreplan.entity.list.TravelListEntity;
import com.koreplan.entity.list.TravelPlanEntity;
import com.koreplan.repository.list.TravelListRepository;
import com.koreplan.repository.list.TravelPlanRepository;

@Service
public class TravelPlanService {
	
	@Autowired
	private TravelListRepository travelListRepository;
	
	@Autowired
	private TravelPlanRepository travelPlanRepository;
	
	// plans 전체로 저장(추가)
	public TravelPlanEntity addPlan(TravelPlanEntity travelPlan) {
		
		List<TravelListEntity> travelLists = travelPlan.getTravelLists();
		
		// 기존 day 순 + order 순 정렬 
		for (int i = 0; i < travelLists.size() - 1; i++) {
			for(int j = i + 1; j < travelLists.size(); j++) {
				TravelListEntity a = travelLists.get(i);
				TravelListEntity b = travelLists.get(j);
				if (a.getDay() > b.getDay() || (a.getDay() == b.getDay() && a.getOrder() > b.getOrder())) {
					travelLists.set(i, b);
					travelLists.set(j, a);
				}
			}
		}
		
	    // 순차적인 day 재부여
	    int currentDay = 1;
	    int lastDayValue = -1;
		
		
		for (TravelListEntity item: travelPlan.getTravelLists()) {
			item.setTravelPlan(travelPlan); // 양방향 연관관계 설정

	        if (item.getDay() != lastDayValue) {  // day 값이 바뀌었으면
	            lastDayValue = item.getDay();
	            item.setDay(currentDay++); // 새로운 day 값 부여
	        } else {
	            item.setDay(currentDay - 1); // 이전과 같은 day면 같은 값 유지
	        }
		}
		return travelPlanRepository.save(travelPlan);
	}
	
	
	// 사용자 계획 조회
	public List<TravelPlanEntity> getPlanListByUserId(Integer userId) {
		return travelPlanRepository.findByUserIdWithLists(userId);
	}
	
	// 삭제
	public void deleteTravelPlanByPlanId (Long planId) {
		travelPlanRepository.deleteById(planId);
	}
	
}
