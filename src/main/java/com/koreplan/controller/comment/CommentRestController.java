package com.koreplan.controller.comment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.koreplan.dto.comment.CommentResponseDto;
import com.koreplan.service.comment.CommentService;

import lombok.extern.slf4j.Slf4j;
@RequestMapping("/api/spot")
@RestController
@Slf4j
@CrossOrigin(origins = "*", allowCredentials = "false")  // allowCredentials를 false로 명시
public class CommentRestController {
	@Autowired
	private CommentService commentService;
	
	@GetMapping("/test")
    public String test() {
        return "API 연결 성공!";
    }
	@GetMapping("/{contentId}/comment")
	public ResponseEntity<?> getSpotInfo(@PathVariable String contentId){
		if (contentId == null || contentId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("contentId가 필요합니다.");
        }
		try {
			CommentResponseDto comment =commentService.getInfomation(contentId);
			 return ResponseEntity.ok(comment);
		}catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
	}
}
