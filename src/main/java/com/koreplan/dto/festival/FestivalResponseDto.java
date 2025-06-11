package com.koreplan.dto.festival;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.koreplan.entity.festival.FestivalEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
//내 DB에서 프론트로 보낼 때 사용할 dto
public class FestivalResponseDto {
    
    private String contentId;           // 콘텐츠 ID
    private Integer contentTypeId;      // 콘텐츠 타입 ID (15 고정)
    private String title;               // 축제명
    private String addr1;               // 주소
    private String addr2;               // 상세주소
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate eventStartDate;   // 시작일
    
    @JsonFormat(pattern = "yyyy-MM-dd") 
    private LocalDate eventEndDate;     // 종료일
    
    private String firstimage;          // 대표 이미지
    private String firstimage2;         // 썸네일 이미지
    
    private String mapx;                // 경도 (String)
    private String mapy;                // 위도 (String)
    
    private String c1Code;              // 대분류 코드
    private String c2Code;              // 중분류 코드
    private String c3Code;              // 소분류 코드
    private String c2Name;              // 카테고리명 (축제/공연/행사)
    
    private String regionName;          // 지역명 (시도)
    private Long regionCode;            // 지역 코드
    
    private String wardName;            // 시군구명
    private Long wardCode;              // 시군구 코드
    
    private String overview;            // 개요/설명
    private Integer viewCount;          // 조회수
    private String status;              // 진행상태 (진행중/진행예정/종료됨)
    
    // Entity -> DTO 변환 메서드
    public static FestivalResponseDto from(FestivalEntity entity) {
        return FestivalResponseDto.builder()
            .contentId(entity.getContentId())
            .contentTypeId(entity.getContentTypeId())
            .title(entity.getTitle())
            .addr1(entity.getAddr1())
            .addr2(entity.getAddr2())
            .eventStartDate(entity.getEventStartDate())
            .eventEndDate(entity.getEventEndDate())
            .firstimage(entity.getFirstimage())
            .firstimage2(entity.getFirstimage2())
            .mapx(entity.getMapx())
            .mapy(entity.getMapy())
            .c1Code(entity.getC1Code())
            .c2Code(entity.getC2Code())
            .c3Code(entity.getC3Code())
            .c2Name(getCategoryName(entity.getC2Code()))
            .regionName(entity.getRegionCodeEntity() != null ? 
                       entity.getRegionCodeEntity().getName() : null)
            .regionCode(entity.getRegionCodeEntity() != null ? 
                       entity.getRegionCodeEntity().getRegioncode() : null)
            .wardName(entity.getWardCodeEntity() != null ? 
                     entity.getWardCodeEntity().getName() : null)
            .wardCode(entity.getWardCodeEntity() != null ? 
                     entity.getWardCodeEntity().getWardcode() : null)
            .overview(entity.getOverview())
            .viewCount(entity.getViewCount())
            .status(getFestivalStatus(entity))
            .build();
    }
    
    // 상태를 계산하는 헬퍼 메서드
    private static String getFestivalStatus(FestivalEntity entity) {
        LocalDate today = LocalDate.now();
        LocalDate start = entity.getEventStartDate();
        LocalDate end = entity.getEventEndDate();
        
        if (start.isAfter(today)) {
            return "진행예정";
        } else if ((start.isBefore(today) || start.isEqual(today)) && 
                   (end.isAfter(today) || end.isEqual(today))) {
            return "진행중";
        } else {
            return "종료됨";
        }
    }
    
    // 카테고리 코드를 이름으로 변환하는 헬퍼 메서드
    private static String getCategoryName(String c2Code) {
        if (c2Code == null) return null;
        
        return switch (c2Code) {
            case "EV01" -> "축제";
            case "EV02" -> "공연";  
            case "EV03" -> "행사";
            default -> c2Code;
        };
    }
}