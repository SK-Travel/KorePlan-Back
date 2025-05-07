package com.koreplan.email.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailRequestDto {
    private String email; // 받는 사람 이메일
    private String type;  // "gmail" or "naver" 프론트에서 이거 하나만 보내도록!
}
