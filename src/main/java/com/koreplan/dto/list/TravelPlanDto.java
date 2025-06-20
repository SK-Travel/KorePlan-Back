package com.koreplan.dto.list;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class TravelPlanDto {
    private Integer userId;
    private String title;
    private List<TravelDataDto> travelLists;
    private LocalDate startDate;
    private LocalDate endDate;
    //date 추가해야됨
	
}
