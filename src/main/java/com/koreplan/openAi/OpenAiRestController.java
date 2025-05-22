package com.koreplan.openAi;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.koreplan.openAi.dto.MessageRequest;
import com.koreplan.openAi.service.OpenAiService;

@RestController
@RequestMapping("/api/openai")
public class OpenAiRestController {
	
	private final OpenAiService openAiService;
	
	// 생성자 주입 방식으로 OpenAiService 연결
	public OpenAiRestController(OpenAiService openAiService) {
		this.openAiService = openAiService;
	}
	
	/**
	 * 
	 * @param request 사용자가 보낸 질문
	 * @return GPT의 응답
	 */
	@PostMapping("/ask")
	public String askGpt(@RequestBody MessageRequest request) {
		return openAiService.chatWithGpt(request.getMessage());
	}	

}
