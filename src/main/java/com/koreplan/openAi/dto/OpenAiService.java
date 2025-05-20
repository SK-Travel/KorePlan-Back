package com.koreplan.openAi.dto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;

@Service
public class OpenAiService {
	
	// API키 읽기
	@Value("${openai.api.key}")
	private String openaiApiKey;
	
	private WebClient webClient;
	
	
	 // 빈 생성 후 초기화 메서드 (WebClient 설정)
	@PostConstruct
	public void init() {
		// WebClient 빌더를 통해 OpenAI API 기본 URL과 헤더 설정
		// Authorization 헤더에 Bearer + API 키를 넣음.
		webClient = WebClient.builder()
				.baseUrl("https://api.openai.com/v1") // OPEN API 기본 URL
				.defaultHeader("Autorization", "Bearer " + openaiApiKey) // 인증 헤더
				.defaultHeader("Content-Type", "Application/json") // 인증타입
				.build();
	}
	
	public String chatWithGpt(String userMessage) {
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
				.bodyToMono(String.class) // MONO<String> 타입으로 반환받음.
				.block(); // 동기 호출 (응답 올 때까지 기다림)
	}
}
