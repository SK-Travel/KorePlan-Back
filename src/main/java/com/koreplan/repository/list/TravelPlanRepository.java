package com.koreplan.repository.list;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.koreplan.entity.list.TravelPlanEntity;

@Repository
public interface TravelPlanRepository extends JpaRepository<TravelPlanEntity, Long> {
	List<TravelPlanEntity> findByUserEntity_id(Integer userId);
}

