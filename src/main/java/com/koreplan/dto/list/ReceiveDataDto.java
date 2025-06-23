package com.koreplan.dto.list;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ReceiveDataDto {
    private Long dataId;    
    private Integer day;     
    private Integer order;  
    
}
