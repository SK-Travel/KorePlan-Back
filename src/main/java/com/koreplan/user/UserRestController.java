package com.koreplan.user;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.koreplan.user.dto.UserDTO;
import com.koreplan.user.entity.UserEntity;

@RestController
@RequestMapping("/api")
public class UserRestController {
	
	@Autowired
	private UserDTO userDto;
	
	// 테스트용
    @GetMapping("/user")
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
    
    // 비밀번호 체크 API
    @PostMapping("/user/check-password")
    public Map<String, Object> checkPassword(@RequestParam("loginId") String loginId,
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
