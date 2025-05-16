package com.koreplan.common;

import java.security.SecureRandom;
import java.util.Base64;

public class JwtKeyGenerator {
	
	// jwt.secretkey생성 한 번만 씀
    public static void main(String[] args) {
        byte[] key = new byte[32]; // 256비트 = 32바이트
        new SecureRandom().nextBytes(key);
        String base64Key = Base64.getEncoder().encodeToString(key);
        System.out.println("Base64 JWT Secret Key: " + base64Key);
    }
}
