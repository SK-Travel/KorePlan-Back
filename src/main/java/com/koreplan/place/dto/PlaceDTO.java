package com.koreplan.place.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlaceDTO {

    private String title;         // 장소명 
    private String link;          // 장소 링크 
    private String category;      // 카테고리
    private String address;       // 지번 주소
    private String roadAddress;   // 도로명 주소
    private String telephone;     // 전화번호 (없을 수도 있음)
    private String mapx;          // TM128 기준 경도 (x)
    private String mapy;          // TM128 기준 위도 (y)
}
