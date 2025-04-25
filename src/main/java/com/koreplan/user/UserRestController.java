package com.koreplan.user;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.koreplan.user.dto.UserDTO;
import com.koreplan.user.entity.UserEntity;

@RestController
public class UserRestController {
	
	@Autowired
	private UserDTO userDto;
	
	
    @GetMapping("/api/user")
    public Map<String, Object> getUser() {
    	
    	int id = 1;
    	UserEntity user = userDto.getUserEntityById(id);
    	
    	System.out.println("✅ user: " + user);
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
}
