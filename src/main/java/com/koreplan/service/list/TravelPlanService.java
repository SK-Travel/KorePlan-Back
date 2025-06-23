package com.koreplan.service.list;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koreplan.data.repository.DataRepository;
import com.koreplan.data.service.SearchDataService;
import com.koreplan.dto.list.AIDataDto;
import com.koreplan.dto.list.AIPlanDto;
import com.koreplan.dto.list.DataSearchDto;
import com.koreplan.dto.list.SendDataDto;
import com.koreplan.dto.list.SendTravelPlanDto;
import com.koreplan.entity.list.TravelDataEntity;
import com.koreplan.entity.list.TravelPlanEntity;
import com.koreplan.repository.list.TravelPlanRepository;
import com.koreplan.user.entity.UserEntity;
import com.koreplan.user.repository.UserRepository;

@Service
@Transactional
public class TravelPlanService {
	
	@Autowired
	private TravelPlanRepository travelPlanRepository;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private DataRepository dataRepository;
	
	@Autowired
	private SearchDataService searchDataService;
	
    // 나만의 리스트에서 검색 로직
	public List<DataSearchDto> searchDataByKeyword(String keyword) {
		
	    if (keyword == null || keyword.trim().isEmpty()) {
	        return Collections.emptyList();
	    }

	    String trimmedKeyword = keyword.trim();

	    // searchByKeywordList가 DTO 리스트를 반환하도록 호출
	    return searchDataService.searchByKeywordList(trimmedKeyword);
	}
    
	// AI로 plans 전체로 저장(추가)
	public TravelPlanEntity addAiPlan(AIPlanDto dto) {
		Optional<UserEntity> userOpt = userRepository.findById(dto.getUserId());
	    if (userOpt.isEmpty()) {
	        throw new RuntimeException("User not found with id: " + dto.getUserId());
	    }
	    
	    UserEntity user = userOpt.get();
		
		TravelPlanEntity travelPlanEntity = new TravelPlanEntity();
		travelPlanEntity.setUserEntity(user);
		travelPlanEntity.setTitle(dto.getTitle());
		travelPlanEntity.setStartDate(dto.getStartDate());
		travelPlanEntity.setEndDate(dto.getEndDate());
		
		List<AIDataDto> travelDataDto = dto.getTravelLists();
	
		for (AIDataDto dto2 : travelDataDto) {
			TravelDataEntity travelDataEntity = new TravelDataEntity();
			
			travelDataEntity.setDataEntity(dataRepository.findById(dto2.getId()).orElseThrow(() -> new RuntimeException("Data not found with id: " + dto2.getId())));
			travelDataEntity.setDay(dto2.getDay());
			travelDataEntity.setOrder(dto2.getOrder());
			travelDataEntity.setTravelPlan(travelPlanEntity);
			
			travelPlanEntity.getTravelDataList().add(travelDataEntity);
		}
		return travelPlanRepository.save(travelPlanEntity) ;
	}
	
	// 나만의 리스트 만들기
	public TravelPlanEntity addOwnPlan(SendTravelPlanDto dto) {
		Optional<UserEntity> userOpt = userRepository.findById(dto.getUserId());
	    if (userOpt.isEmpty()) {
	        throw new RuntimeException("User not found with id: " + dto.getUserId());
	    }
	    
	    UserEntity user = userOpt.get();
		
		TravelPlanEntity travelPlanEntity = new TravelPlanEntity();
		travelPlanEntity.setUserEntity(user);
		travelPlanEntity.setTitle(dto.getTitle());
		travelPlanEntity.setStartDate(dto.getStartDate());
		travelPlanEntity.setEndDate(dto.getEndDate());
//		travelPlanEntity.setStartDate(LocalDate.parse("2025-06-18"));
//		travelPlanEntity.setEndDate(LocalDate.parse("2025-06-19"));
		
		
		List<SendDataDto> travelDataDto = dto.getSendDataDto();
		
		for (SendDataDto dto2 : travelDataDto) {
			TravelDataEntity travelDataEntity = new TravelDataEntity();
			
			travelDataEntity.setDataEntity(dataRepository.findById(dto2.getId()).orElseThrow(() -> new RuntimeException("Data not found with id: " + dto2.getId())));
			travelDataEntity.setDay(dto2.getDay());
			travelDataEntity.setOrder(dto2.getOrder());
			travelDataEntity.setTravelPlan(travelPlanEntity);
			
			travelPlanEntity.getTravelDataList().add(travelDataEntity);
		}
		return travelPlanRepository.save(travelPlanEntity) ;
	}
	
	
	
	
	// 사용자 계획 조회
	@Transactional(readOnly = true)
    public List<SendTravelPlanDto> getPlanListDtoByUserId(Integer userId) {
        List<TravelPlanEntity> plans = travelPlanRepository.findByUserEntity_id(userId);
        List<SendTravelPlanDto> sendAllTravelPlanDtoList = new ArrayList<>();
        for (TravelPlanEntity p : plans) {
            SendTravelPlanDto sendTravelDto = new SendTravelPlanDto();
            
            // 여기서 Lazy Loading 수행 (Session 활성 상태)
            List<TravelDataEntity> tde = p.getTravelDataList();
            
            List<SendDataDto> finalList = new ArrayList<>();
            
            for (TravelDataEntity a : tde) {
            	SendDataDto sendDataDto = new SendDataDto();
            	//세팅
            	sendDataDto.setTitle(a.getDataEntity().getTitle());
            	sendDataDto.setDay(a.getDay());
            	sendDataDto.setOrder(a.getOrder());
            	sendDataDto.setMapx(a.getDataEntity().getMapx());
            	sendDataDto.setMapy(a.getDataEntity().getMapy());
            	sendDataDto.setDataId(a.getDataEntity().getId());
            	sendDataDto.setFirstImage(a.getDataEntity().getFirstimage());
            	sendDataDto.setContentId(a.getDataEntity().getContentId());
            	
            	finalList.add(sendDataDto);
            }
            
            sendTravelDto.setSendDataDto(finalList);
            
            sendTravelDto.setId(p.getId());
            sendTravelDto.setTitle(p.getTitle());
            sendTravelDto.setStartDate(p.getStartDate());
            sendTravelDto.setEndDate(p.getEndDate());
            
            sendAllTravelPlanDtoList.add(sendTravelDto);
        }
        
        return sendAllTravelPlanDtoList;
    }
	//사용자의 계획 중 하나만 조회(상세,수정페이지)
	@Transactional(readOnly = true)
	public SendTravelPlanDto getPlanDto(Integer userId,Long planId) {
		Optional<TravelPlanEntity> planEntity = travelPlanRepository.findByIdAndUserEntityId(planId, userId);
		SendTravelPlanDto sendTravelDto = new SendTravelPlanDto();
	
		sendTravelDto.setId(planEntity.get().getId());
        sendTravelDto.setTitle(planEntity.get().getTitle());
        sendTravelDto.setStartDate(planEntity.get().getStartDate());
        sendTravelDto.setEndDate(planEntity.get().getEndDate());
        
		List<TravelDataEntity> planDataEntity = planEntity.get().getTravelDataList();
		
		List<SendDataDto> finalList = new ArrayList<>();
		
		for (TravelDataEntity a : planDataEntity) {
        	SendDataDto sendDataDto = new SendDataDto();
        	//세팅
        	sendDataDto.setTitle(a.getDataEntity().getTitle());
        	sendDataDto.setDay(a.getDay());
        	sendDataDto.setOrder(a.getOrder());
        	sendDataDto.setMapx(a.getDataEntity().getMapx());
        	sendDataDto.setMapy(a.getDataEntity().getMapy());
        	sendDataDto.setDataId(a.getDataEntity().getId());
        	sendDataDto.setFirstImage(a.getDataEntity().getFirstimage());
        	sendDataDto.setContentId(a.getDataEntity().getContentId());
        	
        	finalList.add(sendDataDto);
        }
		sendTravelDto.setSendDataDto(finalList);
		
		return sendTravelDto;
	}
	// 삭제
	public void deleteTravelPlanByPlanId (Long planId) {
		travelPlanRepository.deleteById(planId);
	}
	
}
