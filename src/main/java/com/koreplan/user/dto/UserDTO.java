package com.koreplan.user.dto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.koreplan.user.entity.UserEntity;
import com.koreplan.user.repository.UserRepository;

@Service
public class UserDTO {
	
	@Autowired
	private UserRepository userRepository;
	
	// 유저 조회
	public UserEntity getUserEntityById(int id) {
		return userRepository.findById(id).orElse(null);
	}
	
	// 비밀번호 조회
	public UserEntity getEntityByLoginId(String loginId) {
		return userRepository.findByLoginId(loginId).orElse(null);
	}
	
}
