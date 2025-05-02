package com.koreplan.user.dto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.koreplan.user.entity.UserEntity;
import com.koreplan.user.repository.UserRepository;

@Service
public class UserDTO {
	
	@Autowired
	private UserRepository userRepository;
	
	// 유저 조회 테스트용
	public UserEntity getUserEntityById(int id) {
		return userRepository.findById(id).orElse(null);
	}
	
	// 회원가입용 아이디 중복확인 조회
	public UserEntity getUserEntityByLoginId(String loginId) {
		return userRepository.findByLoginId(loginId).orElse(null);
	}
	
	
	// 회원수정용 비밀번호 존재여부 조회
	public UserEntity getEntityByLoginId(String loginId) {
		return userRepository.findByLoginId(loginId).orElse(null);
	}
	
}
