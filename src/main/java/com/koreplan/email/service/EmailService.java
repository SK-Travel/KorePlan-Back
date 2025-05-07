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
		
	    if (request.getEmail() == null || request.getCode() == null) {
	        return false;
	    }
	    
	    String existingCode = verificationCodes.get(request.getEmail());
	    if (existingCode == null) {
	        return false;
	    }

	    boolean result = existingCode.equals(request.getCode());
	    verificationCodes.put(request.getEmail(), ""); // 코드 초기화
	    return result;
	}
	
}
