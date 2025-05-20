//package com.koreplan.area.dto;
//
//import java.util.List;
//
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//public class ResponseDto {
//    private Response response;
//}
//
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//class Response {
//    private Header header;
//    private Body body;
//}
//
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//class Header {
//    private String resultCode;
//    private String resultMsg;
//}
//
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//class Body {
//    private Items items;
//    private int numOfRows;
//    private int pageNo;
//    private int totalCount;
//}
//
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//class Items {
//    private List<Item> item;
//}
//
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//class Item {
//    private int rnum;
//    private int code; 
//    private String name;
//}
// ResponseDto.java
package com.koreplan.area.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDto {
    private Response response;
}
