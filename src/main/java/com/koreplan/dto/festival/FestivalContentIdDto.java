package com.koreplan.dto.festival;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class FestivalContentIdDto {
	
	@JsonProperty("contentid")  // API에서 오는 실제 필드명
    private String contentId;
}
