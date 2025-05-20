package com.koreplan.category.dto;

import java.util.List;

import com.koreplan.area.dto.Item;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
        private List<CategoryDto> item;
    }
    @Data
    public class Item {
        private int rnum;
        private int code;
        private String name;
    }

    
}
