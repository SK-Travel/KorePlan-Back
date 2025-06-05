package com.koreplan.dto.detail;

import lombok.Data;

// 문화시설 정보 DTO (contentTypeId: 14)
@Data
public class CultureInfoDto {
    private String contentid;
    private String contenttypeid;
    private String scale;
    private String usefee;
    private String discountinfo;
    private String spendtime;
    private String parkingfee;
    private String infocenterculture;
    private String accomcountculture;
    private String usetimeculture;
    private String restdateculture;
    private String parkingculture;
    private String chkbabycarriageculture;
    private String chkpetculture;
    private String chkcreditcardculture;
}