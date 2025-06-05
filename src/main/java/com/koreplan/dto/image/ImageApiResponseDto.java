package com.koreplan.dto.image;

import java.util.List;

import lombok.Data;

//ImageApiResponseDto.java
@Data
public class ImageApiResponseDto {
	private Response response;

	@Data
	public static class Response {
		private Header header;
		private Body body;
	}

	@Data
	public static class Header {
		private String resultCode;
		private String resultMsg;
	}

	@Data
	public static class Body {
		private Items items;
		private int numOfRows;
		private int pageNo;
		private int totalCount;
	}

	@Data
	public static class Items {
		private List<ImageInfoDto> item;
	}
}