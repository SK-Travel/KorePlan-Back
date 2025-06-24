package com.koreplan.dto.list;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ReceiveTravelPlanDto {
	private Long id;
    private int userId;
    private String title;
    private LocalDate startDate;  
    private LocalDate endDate;
    private List<ReceiveDataDto> travelLists;
	
}


