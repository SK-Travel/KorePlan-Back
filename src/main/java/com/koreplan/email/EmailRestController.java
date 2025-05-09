package com.koreplan.email;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.koreplan.email.dto.EmailRequestDto;
import com.koreplan.email.dto.VerifyRequestDto;
import com.koreplan.email.service.EmailService;

@RestController
@RequestMapping("/api/email")
public class EmailRestController {
	
	private final EmailService emailService;
	
    // 생성자 주입 (의존성 주입)
    public EmailRestController(EmailService emailService) {
        this.emailService = emailService;
    }
	
    // 인증 코드 보내기 요청 코드
    @PostMapping("/send-code")
    public ResponseEntity<?> sendVerificationCode(
    		@RequestBody EmailRequestDto request) {
    	boolean result = emailService.sendEmail(request); // 이메일 요청 보내기
    	return result ? ResponseEntity.ok().build() : ResponseEntity.status(500).build(); // result가 true면 200(ok), 아니면 에러 500리턴
    }
    
    // 이메일 확인 코드
    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(
    		@RequestBody VerifyRequestDto request) {
    	boolean result = emailService.verifyCode(request);
    	return result ? ResponseEntity.ok().build() : ResponseEntity.status(400).body("Invalid Code") ; // 인증번호가 true면 200(ok), false면 400 리턴
    }
	
}
