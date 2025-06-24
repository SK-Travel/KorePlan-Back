package com.koreplan.service.list;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koreplan.data.entity.DataEntity;
import com.koreplan.data.repository.DataRepository;
import com.koreplan.data.service.SearchDataService;
import com.koreplan.dto.list.AIDataDto;
import com.koreplan.dto.list.AIPlanDto;
import com.koreplan.dto.list.DataSearchDto;
import com.koreplan.dto.list.ReceiveDataDto;
import com.koreplan.dto.list.ReceiveTravelPlanDto;
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
	public TravelPlanEntity addOwnPlan(ReceiveTravelPlanDto dto) {
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
		
		List<ReceiveDataDto> travelDataDto = dto.getTravelLists();
		
		for (ReceiveDataDto dto2 : travelDataDto) {
			TravelDataEntity travelDataEntity = new TravelDataEntity();
			
			travelDataEntity.setDataEntity(dataRepository.findById(dto2.getDataId()).orElseThrow(() -> new RuntimeException("Data not found with id: " + dto2.getDataId())));
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
        	sendDataDto.setAddr(a.getDataEntity().getAddr1());
        	finalList.add(sendDataDto);
        }
		sendTravelDto.setSendDataDto(finalList);
		
		return sendTravelDto;
	}
	
	
	// 수정페이지에서 plan삭제
	// 특정 장소 삭제 (단일 장소 삭제)
	public boolean deleteSingleLocationFromPlan(Long planId, Long dataId, int day, int order, Long userId) {
	    // 1. 해당 Plan 찾기
	    Optional<TravelPlanEntity> planOpt = travelPlanRepository.findById(planId);

	    if (planOpt.isEmpty()) return false;

	    TravelPlanEntity plan = planOpt.get();

	    // 2. 사용자 권한 확인
	    if (plan.getUserEntity().getId() != userId) return false;

	    // 3. 데이터 리스트에서 대상 찾기 (for문)
	    List<TravelDataEntity> dataList = plan.getTravelDataList();
	    TravelDataEntity toDelete = null;

	    for (TravelDataEntity td : dataList) {
	        if (td.getDataEntity().getId().equals(dataId) && td.getDay() == day && td.getOrder() == order) {
	            toDelete = td;
	            break;
	        }
	    }

	    // 4. 못 찾으면 false 반환
	    if (toDelete == null) return false;

	    // 5. 삭제
	    dataList.remove(toDelete);
	    
	    return true;
	}
	

	// 전체 수정 버튼
	  public boolean updatePlan(ReceiveTravelPlanDto travelPlanDto) {
        // 1. Plan 조회
	    if (travelPlanDto.getId() == null) {
	        throw new IllegalArgumentException("수정할 Plan ID가 null입니다.");
	    }

	    TravelPlanEntity travelPlan = travelPlanRepository.findById(travelPlanDto.getId())
	        .orElseThrow(() -> new IllegalArgumentException("해당 ID의 여행 계획이 없습니다."));

        // 2. 사용자 권한 체크 (int -> Long 변환 주의)
        if (travelPlan.getUserEntity().getId() != travelPlanDto.getUserId()) {
            return false;
        }

        // 3. 제목, 날짜 업데이트
        travelPlan.setTitle(travelPlanDto.getTitle());
        travelPlan.setStartDate(travelPlanDto.getStartDate());
        travelPlan.setEndDate(travelPlanDto.getEndDate());

        // 4. 기존 여행 데이터 모두 삭제
        travelPlan.getTravelDataList().clear();
        travelPlanRepository.saveAndFlush(travelPlan); // ← 안정성 확보
        List<ReceiveDataDto> datas =travelPlanDto.getTravelLists();
        for (ReceiveDataDto dto : datas) {
            DataEntity dataEntity = dataRepository.findById(dto.getDataId())
                    .orElseThrow(() -> new RuntimeException("Invalid dataId: " + dto.getDataId()));

            TravelDataEntity travelData = TravelDataEntity.builder()
                    .dataEntity(dataEntity)
                    .day(dto.getDay())
                    .order(dto.getOrder())
                    .travelPlan(travelPlan)
                    .build();

            travelPlan.getTravelDataList().add(travelData); // ★ add로 넣어주기
        }


        // 6. 저장 (cascade 로 travelData 같이 저장됨)
        travelPlanRepository.save(travelPlan);

        return true;
    }
	
	
	
	
	// 리스트 자체를 삭제
	public void deleteTravelPlanByPlanId (Long planId) {
		travelPlanRepository.deleteById(planId);
	}
	
}
