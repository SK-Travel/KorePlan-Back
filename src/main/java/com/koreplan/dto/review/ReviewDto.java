package com.koreplan.dto.review;

import lombok.Data;

@Data
public class ReviewDto {

	private int userid;
	private long dataid;
	private String contentId;
	private String comment;
	private int rate;
	
}
