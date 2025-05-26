package com.koreplan.data.dto;

import com.koreplan.data.entity.DataEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataResponseDto {
    
    private Long id;
    private String contentId;
    private String addr1;
    private String addr2;
    private String mapx;
    private String mapy;
    private String title;
    private String c1Code;
    private String c2Code;
    private String c3Code;
    private String firstimage;
    private String firstimage2;
    private String tel;
    
    // 연관관계 정보
    private String regionName;
    private Long regionCode;
    private String wardName;
    private Long wardCode;
    
    // Entity에서 DTO로 변환하는 정적 메서드
    public static DataResponseDto fromEntity(DataEntity entity) {
        return DataResponseDto.builder()
            .id(entity.getId())
            .contentId(entity.getContentId())
            .addr1(entity.getAddr1())
            .addr2(entity.getAddr2())
            .mapx(entity.getMapx())
            .mapy(entity.getMapy())
            .title(entity.getTitle())
            .c1Code(entity.getC1Code())
            .c2Code(entity.getC2Code())
            .c3Code(entity.getC3Code())
            .firstimage(entity.getFirstimage())
            .firstimage2(entity.getFirstimage2())
            .tel(entity.getTel())
            // 연관관계 엔티티에서 정보 추출
            .regionName(entity.getRegionCodeEntity() != null ? 
                       entity.getRegionCodeEntity().getName() : null)
            .regionCode(entity.getRegionCodeEntity() != null ? 
                       entity.getRegionCodeEntity().getRegioncode() : null)
            .wardName(entity.getWardCodeEntity() != null ? 
                     entity.getWardCodeEntity().getName() : null)
            .wardCode(entity.getWardCodeEntity() != null ? 
                     entity.getWardCodeEntity().getWardcode() : null)
            .build();
    }
}
