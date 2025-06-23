package com.koreplan.dto.list;

import lombok.Data;

@Data

// 나만의 리스트에서 장소검색할 때 필요한 것
public class DataSearchDto {
    private Long id;
    private String title;
    private String regionName;  // RegionCodeEntity의 이름만 전달
    private String firstimage;
}