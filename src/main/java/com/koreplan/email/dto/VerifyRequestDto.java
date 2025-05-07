package com.koreplan.email.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyRequestDto {
    private String email;   // 인증하려는 이메일
    private String code;    // 입력한 인증 코드
}
	