package com.koreplan.openAi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.koreplan.openAi.dto.OpenAiService;

@RestController
@RequestMapping("/api/openai")
public class OpenAiRestController {
	
	private final OpenAiService openAiService;
	
	// 생성자 주입 방식으로 OpenAiService 연결
	public OpenAiRestController(OpenAiService openAiService) {
		this.openAiService = openAiService;
	}
	
	
	
	
}
