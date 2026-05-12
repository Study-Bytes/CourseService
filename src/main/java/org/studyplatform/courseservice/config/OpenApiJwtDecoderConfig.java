package org.studyplatform.courseservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

@Configuration
@Profile("openapi")
public class OpenApiJwtDecoderConfig {

    @Bean
    public JwtDecoder openApiJwtDecoder() {
        return token -> {
            throw new JwtException("JWT decoding is disabled in the OpenAPI generation profile.");
        };
    }
}
