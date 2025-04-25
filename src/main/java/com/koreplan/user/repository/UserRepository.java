package com.koreplan.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koreplan.user.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Integer>{
	
	//유저 조회
	 Optional<UserEntity> findById(int id);
}
