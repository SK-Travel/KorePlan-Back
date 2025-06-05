package com.koreplan.dto.comment;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
@Data
public class CommentDto {
	@JsonProperty("contentid")
    private String contentId;
    
    @JsonProperty("contenttypeid")
    private String contentTypeId;
    
    private String title;
    
    @JsonProperty("createdtime")
    private String createdTime;
    
    @JsonProperty("modifiedtime")
    private String modifiedTime;
    
    private String tel;
    
    @JsonProperty("telname")
    private String telName;
    
    private String homepage;
    
    @JsonProperty("firstimage")
    private String firstImage;
    
    @JsonProperty("firstimage2")
    private String firstImage2;
    
    @JsonProperty("cpyrhtDivCd")
    private String copyrightDivisionCode;
    
    @JsonProperty("areacode")
    private String areaCode;
    
    @JsonProperty("sigungucode")
    private String sigunguCode;
    
    @JsonProperty("lDongRegnCd")
    private String legalDongRegionCode;
    
    @JsonProperty("lDongSignguCd")
    private String legalDongSigunguCode;
    
    @JsonProperty("lclsSystm1")
    private String classificationSystem1;
    
    @JsonProperty("lclsSystm2")
    private String classificationSystem2;
    
    @JsonProperty("lclsSystm3")
    private String classificationSystem3;
    
    private String cat1;
    
    private String cat2;
    
    private String cat3;
    
    private String addr1;
    
    private String addr2;
    
    @JsonProperty("zipcode")
    private String zipCode;
    
    @JsonProperty("mapx")
    private String mapX;
    
    @JsonProperty("mapy")
    private String mapY;
    
    @JsonProperty("mlevel")
    private String mapLevel;
    
    private String overview;
    
}
