package com.koreplan.dto.list;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SendDataDto {
	private String title;
    private Integer day;
    private Integer order;
    private String mapx;
    private String mapy;
    private Long dataId;
    private String firstImage;
    private String contentId;
	
}
