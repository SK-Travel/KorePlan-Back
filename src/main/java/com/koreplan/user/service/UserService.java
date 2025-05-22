package com.koreplan.user.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.koreplan.common.EncryptUtils;
import com.koreplan.user.entity.UserEntity;
import com.koreplan.user.repository.UserRepository;

@Service
public class UserService {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private EncryptUtils encryptUtils;
	
	// 유저 조회 테스트용
	public UserEntity getUserEntityById(int id) {
		return userRepository.findById(id).orElse(null);
	}
	
	// 로그인아이디로 유저 조회
	public UserEntity getUserEntityByLoginId(String loginId) {
		return userRepository.findByLoginId(loginId).orElse(null);
	}
	
    // 이메일로 유저 Optional 조회 (중복 방지용)
    public Optional<UserEntity> getUserEntityByEmailOptional(String email) {
        return userRepository.findByEmail(email);
    }
	
	// 회원가입(일반 구글 통합)
	public UserEntity signUpUser(UserEntity userEntity) {
		// 중복 체크 컨트롤러에서
		
		// 비밀번호 처리
		String rawPassword = userEntity.getPassword();
		String hashedPassword;
		
		if (rawPassword == null || rawPassword.isBlank() || "oauth".equals(rawPassword)) {
			hashedPassword = encryptUtils.hashPassword("oauth"); // Google OAuth 사용자
		} else {
			hashedPassword = encryptUtils.hashPassword(rawPassword); // 일반 사용자
		}
		
		
		// 유저 저장
		return userRepository.save(UserEntity.builder()
				.loginId(userEntity.getLoginId())
				.password(hashedPassword)
				.name(userEntity.getName())
				.email(userEntity.getEmail())
				.phoneNumber(userEntity.getPhoneNumber())
				.build());
		
	}
	
	// 회원가입 (직접 호출 방식, 내부적으로는 위 로직 사용, 원래 회원가입 시)
    public UserEntity addUser(String loginId, String password, String name, String email, String phoneNumber) {
        return userRepository.save(UserEntity.builder()
                .loginId(loginId)
                .password(password)
                .name(name)
                .email(email)
                .phoneNumber(phoneNumber)
                .build());
    }
	
    // 로컬 스토리지에 저장된 유저 정보 조회
    public UserEntity getUSerEntityByNameEmail(String name, String email) {
    	return userRepository.findByNameAndEmail(name, email);
    }
    
    
	
}
