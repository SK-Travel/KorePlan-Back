package com.koreplan.repository.list;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.koreplan.entity.list.TravelPlanEntity;

@Repository
public interface TravelPlanRepository extends JpaRepository<TravelPlanEntity, Long> {
    @Query("select distinct tp from TravelPlanEntity tp left join fetch tp.travelLists where tp.userId = :userId")
    List<TravelPlanEntity> findByUserIdWithLists(@Param("userId") Integer userId);
}

