package org.studyplatform.courseservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String INTERNAL_API_KEY_SCHEME = "internalApiKey";

    @Bean
    public OpenAPI courseServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CourseService API")
                        .version("v1")
                        .description("Course content and structure service API."))
                .components(new Components()
                        .addSecuritySchemes(
                                INTERNAL_API_KEY_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-Internal-Api-Key")
                                        .description("Static service-to-service API key for internal CourseService endpoints. This is an MVP mechanism and should be replaced or hardened for production.")
                        ));
    }
}
