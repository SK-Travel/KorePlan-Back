package com.koreplan.user;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.koreplan.common.EncryptUtils;
import com.koreplan.user.dto.UserDTO;
import com.koreplan.user.entity.UserEntity;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/user")
public class UserRestController {
	
	@Autowired
	private UserDTO userDto;
	
	@Autowired
	private EncryptUtils encryptUtils;
	
	// 테스트용
    @GetMapping("")
    public Map<String, Object> getUser() {
    	
    	int id = 1;
    	UserEntity user = userDto.getUserEntityById(id);
    	

        Map<String, Object> result = new HashMap<>();
        if (user != null) {
            result.put("result", "성공");
            result.put("user", user);
        } else {
            result.put("result", "실패");
            result.put("message", "해당 유저가 존재하지 않습니다.");
        }


        return result;
    }
    
    // 회원가입 ID중복확인 API
    /**
     * 
     * @param loginId
     * @return
     */
    @PostMapping("/is-duplicated-id")
    public Map<String, Object> isDuplicatedId(
    		@RequestParam("loginId") String loginId) {
    	
    	// db 조회
    	UserEntity user = userDto.getUserEntityByLoginId(loginId);
    	
    	Map<String, Object> result = new HashMap<>();
    	
    	if (user != null) {
    		result.put("code", 200);
    		result.put("is_duplicated_id", true);
    	} else {
    		result.put("is_duplicated_id", false);
    	}
    	
    	
    	return result;
    }
    
    // 회원가입 
    /**
     * 
     * @param userEntity
     * @return
     */
    @PostMapping("/sign-up")
    public Map<String, Object> signUp(
    		@RequestBody UserEntity userEntity) {
    	
    	// 해싱된 비밀번호
    	String hashedPassword = encryptUtils.hashPassword(userEntity.getPassword());
    	
    	// user Insert
    	UserEntity user = userDto.addUser(userEntity.getLoginId(), hashedPassword, userEntity.getName(), userEntity.getEmail(), userEntity.getPhoneNumber());
    	
    	Map<String, Object> result = new HashMap<>();
    	if (user != null) {
    		result.put("code", 200);
    		result.put("result", "성공");
    	} else {
    		result.put("code", 500);
    		result.put("error_message", "회원가입에 실패했습니다.");
    	}
    	return result;
    }
    
    
    // 로그인 (세션 저장하기== 비로그인/로그인 시)
    /**
     * 
     * @param userEntity
     * @param session
     * @return
     */

   	@PostMapping("/sign-in")
    public Map<String, Object> signIn (
    		@RequestBody UserEntity userEntity,
    		HttpSession session) {
   		
   		
   		//응답값 저장
   		Map<String, Object> result = new HashMap<>();
    	
    	// 해싱된 비밀번호 찾기 위해 loginId로 유저 찾기
   		UserEntity savedUser = userDto.getUserEntityByLoginId(userEntity.getLoginId());
   		
   		// 유저 없을 때
   		if (savedUser == null) {
   			result.put("code", 400);
   			result.put("error_message", "존재하지 않은 사용자입니다.");
   			return result;
   		}
   		
   		
   		// 해당 유저의 해싱 비번 저장
    	String hashedPassword = savedUser.getPassword();
    	// 비밀번호 일치 로직
    	boolean isPasswordMatch = encryptUtils.checkPassword(userEntity.getPassword(), hashedPassword);
    	// 유저가 있을 때, 비밀번호 일치 시 
    	if (isPasswordMatch) {
			// 정보 저장 session에
			session.setAttribute("userId", savedUser.getLoginId());
			session.setAttribute("name", savedUser.getName());
			
			result.put("code", 200);
			result.put("result", "로그인 성공, 메인페이지로 이동합니다.");
		} else {
			result.put("code", 401);
			result.put("error_message", "비밀번호가 틀렸습니다.");
    	}
    	
    	return result;
    }
    
    
    
  
    // 회원 정보 수정 비밀번호 체크 API
    @PostMapping("/check-password")
    public Map<String, Object> checkPassword(
    		@RequestParam("loginId") String loginId,
    		@RequestParam("password") String password) {
    	
    	UserEntity user = userDto.getEntityByLoginId(loginId);
    	
    	Map<String, Object> result = new HashMap<>();
    	if (user != null) {
    		if (user.getPassword().equals(password)) {
    			result.put("code", 200); // 비밀번호 일치
    			result.put("result", "비밀번호 조회 완료");
    		} else {
    			result.put("code", 400); // 비밀번호 불일치
    			result.put("result", "비밀번호 불일치");
    		}
    	} else {
    		result.put("code", 500);
    		result.put("result", "비밀번호 조회 불가");
    	}
    	return result;
    }
    
}
