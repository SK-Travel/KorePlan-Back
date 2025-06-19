package com.koreplan.service.list;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koreplan.data.repository.DataRepository;
import com.koreplan.dto.list.SendDataDto;
import com.koreplan.dto.list.SendTravelPlanDto;
import com.koreplan.dto.list.TravelDataDto;
import com.koreplan.dto.list.TravelPlanDto;
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
	
	
	// plans 전체로 저장(추가)
	public TravelPlanEntity addPlan(TravelPlanDto dto) {
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
		
		
		List<TravelDataDto> travelDataDto = dto.getTravelLists();
		
		for (TravelDataDto dto2 : travelDataDto) {
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
//	
	// 삭제
	public void deleteTravelPlanByPlanId (Long planId) {
		travelPlanRepository.deleteById(planId);
	}
	
}
