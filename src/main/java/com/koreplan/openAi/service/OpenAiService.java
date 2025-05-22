package com.koreplan.openAi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.koreplan.openAi.UsageTracker;

import jakarta.annotation.PostConstruct;

@Service
public class OpenAiService {
	
	// API키 읽기
	@Value("${openai.api.key}")
	private String openaiApiKey;
	
	private WebClient webClient;
	
	@Autowired
	private UsageTracker usageTracker;
	
	
	 // 빈 생성 후 초기화 메서드 (WebClient 설정)
	@PostConstruct
	public void init() {
		// WebClient 빌더를 통해 OpenAI API 기본 URL과 헤더 설정
		// Authorization 헤더에 Bearer + API 키를 넣음.
		webClient = WebClient.builder()
				.baseUrl("https://api.openai.com/v1") // OPEN API 기본 URL
				.defaultHeader("Authorization", "Bearer " + openaiApiKey)  // 인증용 헤더
				.defaultHeader("Content-Type", "application/json") // JSON 형식 명시
				.build();
	}
	/**
	 * OpenAI Chat Completion API 호출 메서드
     * @param userMessage 사용자가 입력한 질문
     * @return OpenAI API에서 받은 JSON 응답 문자열
	 */
	public String chatWithGpt(String userMessage) {

		int estimatedInputTokens = userMessage.length() / 4; // 대략 글자수/4
	    int estimatedOutputTokens = 200; // 예상 응답 길이 (자유 조정 가능)
	    
	    if (!usageTracker.canProceed(estimatedInputTokens, estimatedOutputTokens)) {
	        return "월 사용 예산을 초과하였습니다. (예산: $5)";
	    }

	    
		// 요청 바디(JSON) 문자열 생성 (ChatGpt모델 gpt-3.5-turbo모델)
		String requestBody = """
				{
					"model": "gpt-3.5-turbo",
					"messages": [
						{"role": "user", "content": "%s"}
					]
				}
				""".formatted(userMessage);
		
		// POST 방식으로 /chat/completions 호출, 응답을 문자열로 받음
		return webClient.post()
				.uri("/chat/completions")
				.bodyValue(requestBody)
				.retrieve()
				.bodyToMono(String.class) // MONO<String> 타입으로 반환받음.s
				.block(); // 동기 호출 (응답 올 때까지 기다림)
	}
}
