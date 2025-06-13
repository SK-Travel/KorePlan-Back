package com.koreplan.dto.festival;

import lombok.Data;

@Data
public class FestivalDetailInfoDto {
    
    private String infoname;        // "행사소개", "행사내용" 등
    private String infotext;        // 실제 내용
    
}