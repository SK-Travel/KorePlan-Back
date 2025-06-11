package com.koreplan.dto.festival;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class FestivalTermDto {
    @JsonProperty("contentid")
    private String contentId;
    
    @JsonProperty("contenttypeid")
    private String contentTypeID;
    
    @JsonProperty("eventstartdate")
    private String eventStartDate;
    
    @JsonProperty("eventenddate")
    private String eventEndDate;
}

