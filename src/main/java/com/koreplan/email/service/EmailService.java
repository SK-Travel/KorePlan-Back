package com.koreplan.email.service;

import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import com.koreplan.email.dto.EmailRequestDto;
import com.koreplan.email.dto.VerifyRequestDto;

@Service
public class EmailService {
    @Value("${mail.gmail.host}")
    private String gmailHost;
    @Value("${mail.gmail.port}")
    private int gmailPort;
    @Value("${mail.gmail.username}")
    private String gmailUsername;
    @Value("${mail.gmail.password}")
    private String gmailPassword;

    @Value("${mail.naver.host}")
    private String naverHost;
    @Value("${mail.naver.port}")
    private int naverPort;
    @Value("${mail.naver.username}")
    private String naverUsername;
    @Value("${mail.naver.password}")
    private String naverPassword;
	
	private final Map<String, String> verificationCodes = new ConcurrentHashMap<>();
	
	  public boolean sendEmail(EmailRequestDto request) {
	        String code = String.valueOf(new Random().nextInt(899999) + 100000);
	        verificationCodes.put(request.getEmail(), code);

	        try {
	            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
	            Properties props = mailSender.getJavaMailProperties();
	            
	            // 이메일 종류에 따라 설정
	            if ("gmail".equalsIgnoreCase(request.getType())) {
	                mailSender.setHost(gmailHost);
	                mailSender.setPort(gmailPort);
	                mailSender.setUsername(gmailUsername);
	                mailSender.setPassword(gmailPassword);
	                props.put("mail.smtp.starttls.enable", "true");
	                props.put("mail.smtp.ssl.enable", "false");
	            } else if ("naver".equalsIgnoreCase(request.getType())) {
	                mailSender.setHost(naverHost);
	                mailSender.setPort(naverPort);
	                mailSender.setUsername(naverUsername);
	                mailSender.setPassword(naverPassword);
	                props.put("mail.smtp.starttls.enable", "false");
	                props.put("mail.smtp.ssl.enable", "true");
	                props.put("mail.smtp.ssl.trust", mailSender.getHost());
	            } else {
	                throw new IllegalArgumentException("지원하지 않는 이메일 유형입니다: " + request.getType());
	            }

	            props.put("mail.smtp.auth", "true");

	            SimpleMailMessage message = new SimpleMailMessage();
	            message.setFrom(mailSender.getUsername());
	            message.setTo(request.getEmail());
	            message.setSubject("Your verification Code");
	            message.setText("Verification: " + code);

	            mailSender.send(message);
	            return true;

	        } catch (Exception e) {
	            e.printStackTrace();
	            return false;
	        }
	    }
	
	// 인증 코드 검증
	public boolean verifyCode(VerifyRequestDto request) {
	    System.out.println("🔍 입력 이메일: " + request.getEmail());
	    System.out.println("🔍 입력 코드: " + request.getCode());
	    
	    
	    // 사용자가 입력한 이메일 주소로 저장된 인증번호를 가져옴.
	    String existingCode = verificationCodes.get(request.getEmail());
	    System.out.println(existingCode);
	    
	    // 저장된 인증번호가 없다면 (인증 요청이 없었거나 만료된 경우) false 반환
	    if (existingCode == null) {
	        return false;
	    }
	    
	    // 저장된 인증번호와 사용자가 입력한 인증번호가 일치하는지 확인
	    if (existingCode.equals(request.getCode())) {
	    	// 인증번호가 일치하면 해당 이메일의 인증번호를 맵에서 삭제 (1회용으로 사용)
	    	verificationCodes.remove(request.getEmail());
	    	return true; // 인증 성공
	    } else {
	    	// 인증번호가 일치하면 삭제하지 않고 flase 반환 -> 사용자 재시도 가능
	    	return false;
	    }
	    
	    
	}
	
}
