package com.koreplan.dto.list;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SendTravelPlanDto {

    private Long id; 
    private int userId;// userId
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<SendDataDto> sendDataDto;
    private List<SendDataDto> deletedDataDto;
}
