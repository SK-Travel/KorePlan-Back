package com.koreplan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;  // 👈 이거 추가!

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("KorePlan API Documentation")
                        .description("KorePlan 프로젝트의 API 문서")
                        .version("1.0"));
    }
}