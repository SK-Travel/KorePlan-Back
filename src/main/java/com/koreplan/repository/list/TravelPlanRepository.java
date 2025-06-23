package com.koreplan.repository.list;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.koreplan.entity.list.TravelPlanEntity;

@Repository
public interface TravelPlanRepository extends JpaRepository<TravelPlanEntity, Long> {
	List<TravelPlanEntity> findByUserEntity_id(Integer userId);
	
	//특정 유저의 단일 여행 계획 조회
    Optional<TravelPlanEntity> findByIdAndUserEntityId(Long id, Integer userId);
}

