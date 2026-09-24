package com.team3.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Origin은 브라우저 페이지 주소다. 배포 프론트와 로컬만 허용한다.
        // sulchedule-server는 API/Swagger 호스트라 제외한다. Try it out이 막히면
        // https 서버 Origin만 다시 넣는다.
        registry.addMapping("/api/**")
            .allowedOrigins("https://sulchedule.sunghoyaaa.com", "http://localhost:3000")
            .allowedMethods("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("Authorization", "Content-Type", "Accept");
    }
}
