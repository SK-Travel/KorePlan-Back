package com.koreplan.dto.detail;

import lombok.Data;

// 관광지 정보 DTO (contentTypeId: 12)
@Data
public class TourismInfoDto {
    private String contentid;
    private String contenttypeid;
    private String heritage1;
    private String heritage2;
    private String heritage3;
    private String infocenter;
    private String opendate;
    private String restdate;
    private String expguide;
    private String expagerange;
    private String accomcount;
    private String useseason;
    private String usetime;
    private String parking;
    private String chkbabycarriage;
    private String chkpet;
    private String chkcreditcard;
}