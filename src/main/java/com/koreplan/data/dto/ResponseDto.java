package com.koreplan.data.dto;

import java.util.List;

import lombok.Data;

@Data
public class ResponseDto {
	
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
    }

    @Data
    public static class Items {
        private List<DataDto> item;
    }
   
    
}