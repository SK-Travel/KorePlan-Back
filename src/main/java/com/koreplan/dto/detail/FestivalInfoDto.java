package com.koreplan.dto.detail;

import lombok.Data;

@Data
public class FestivalInfoDto {

    // 주최/주관 정보
    private String sponsor1;           // 주최자1
    private String sponsor1tel;        // 주최자1 전화번호
    private String sponsor2;           // 주최자2
    private String sponsor2tel;        // 주최자2 전화번호
    
    private String playtime;           // 시간
   
    private String usetimefestival;    // 이용요금
   
}
