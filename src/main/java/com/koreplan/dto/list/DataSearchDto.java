package com.koreplan.dto.list;

import lombok.Data;

@Data

// 나만의 리스트에서 장소검색할 때 필요한 것
public class DataSearchDto {
	private Long id;           // dataId 역할
    private String contentId;  // API ID
    private String title;
    private String regionName;
    private String firstimage;
    
    // 추가할 필드들
    private String mapx;       // 경도
    private String mapy;       // 위도  
    private String addr1;      // 주소
    private String wardName;   // 세부 지역명 (필요하면)
}