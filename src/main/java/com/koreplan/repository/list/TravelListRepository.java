package com.koreplan.repository.list;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.koreplan.entity.list.TravelListEntity;

@Repository
public interface TravelListRepository extends JpaRepository <TravelListEntity, Long> {

}
