package com.koreplan.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koreplan.user.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Integer>{
	
	//유저 조회
	 Optional<UserEntity> findById(int id);
	 
	 // 로그인 ++++ 회원정보 수정용 loginId, 비밀번호로 존재여부 조회
	 Optional<UserEntity> findByLoginId(String loginId);
	 UserEntity findByNameAndEmail(String name, String email);
	 
	 // 구글 회원가입 및 로그인 로직, 이메일로 사용자 찾기 (OAuth2 회원가입 시 중복 가입 방지용)
	 Optional<UserEntity> findByEmail(String email);
	 boolean existsByLoginId(String loginId);
}
