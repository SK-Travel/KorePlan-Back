package com.koreplan.dto.festival;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class FestivalCommonDto {
    @JsonProperty("contentid")      // 추가 필요
    private String contentId;        // 추가 필요
    
    @JsonProperty("title")           // 추가 필요
    private String title;
    
    @JsonProperty("homepage")
    private String homepage;
    
    @JsonProperty("firstimage")      // 추가 필요
    private String firstimage;
    
    @JsonProperty("firstimage2")     // 추가 필요
    private String firstimage2;
    
    @JsonProperty("lDongRegnCd")     // ✅ 이미 있음
    private String lDongRegnCd;
    
    @JsonProperty("lDongSignguCd")   // ✅ 이미 있음
    private String lDongSignguCd;
    
    @JsonProperty("lclsSystm1")      // 추가 필요
    private String lclsSystm1;
    
    @JsonProperty("lclsSystm2")      // 추가 필요
    private String lclsSystm2;
    
    @JsonProperty("lclsSystm3")      // 추가 필요
    private String lclsSystm3;
    
    @JsonProperty("addr1")           // 추가 필요
    private String addr1;
    
    @JsonProperty("addr2")           // 추가 필요
    private String addr2;
    
    @JsonProperty("mapx")            // 추가 필요
    private String mapx;
    
    @JsonProperty("mapy")            // 추가 필요
    private String mapy;
    
    @JsonProperty("overview")        // 추가 필요
    private String overview;
}