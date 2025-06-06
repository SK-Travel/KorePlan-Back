package com.koreplan.openAi;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koreplan.openAi.service.OpenAiService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/openai")
@RequiredArgsConstructor  // final 필드 자동 생성자 주입
public class OpenAiRestController {
	
	private final OpenAiService openAiService;
    private final ObjectMapper mapper = new ObjectMapper();
	
    /**
     * GPT에 질문을 보내고,
     * JSON 배열 형태로 응답받아 DB 필터링 및 보완 후 반환
     */
	@PostMapping("/ask")
    public ResponseEntity<?> askToGpt(@RequestBody Map<String, Object> request) {
        try {

            // 요청 맵을 JSON 문자열로 변환
            String jsonInput = mapper.writeValueAsString(request);

            // OpenAiService를 통해 GPT 호출
            String gptResponse = openAiService.chatWithGpt(jsonInput);

            // GPT 응답 JSON 파싱
            JsonNode gptJsonArray = mapper.readTree(gptResponse);
            
            // 배열 여부 확인
            if (!gptJsonArray.isArray()) {
                return ResponseEntity.badRequest()
                		.body("GPT 응답이 배열이 아닙니다: " + gptResponse);
            }
            

            // GPT가 추천해준 갯수대로
            int gptCount = gptJsonArray.size();
            
            // GPT 추천장소 DB 필터링 + 부족하면 보완까지 한번에
            List<JsonNode> finalPlaces = openAiService.getFilteredAndFilledPlaces(gptJsonArray, gptCount);
            
            return ResponseEntity.ok(finalPlaces);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("GPT 응답 처리 오류: " + e.getMessage());
        }
    }
}
