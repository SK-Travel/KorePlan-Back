package com.koreplan.dto.detail;

import java.util.List;
import lombok.Data;

// 공통 API 응답 DTO (Generic 사용)
@Data
public class ApiResponseDto<T> {
    private Response<T> response;
    
    @Data
    public static class Response<T> {
        private Header header;
        private Body<T> body;
    }
    
    @Data
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }
    
    @Data
    public static class Body<T> {
        private Items<T> items;
        private int numOfRows;
        private int pageNo;
        private int totalCount;
    }
    
    @Data
    public static class Items<T> {
        private List<T> item;
    }
}