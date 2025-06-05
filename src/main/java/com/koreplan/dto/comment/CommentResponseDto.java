package com.koreplan.dto.comment;

import java.util.List;

import com.koreplan.dto.image.ImageApiResponseDto;
import com.koreplan.dto.image.ImageInfoDto;
import com.koreplan.dto.image.ImageApiResponseDto.Body;
import com.koreplan.dto.image.ImageApiResponseDto.Header;
import com.koreplan.dto.image.ImageApiResponseDto.Items;
import com.koreplan.dto.image.ImageApiResponseDto.Response;

import lombok.Data;
@Data
public class CommentResponseDto {
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
		private List<CommentDto> item;
	}
}
