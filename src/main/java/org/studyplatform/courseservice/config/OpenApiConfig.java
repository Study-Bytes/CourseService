package org.studyplatform.courseservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH_SCHEME = "bearerAuth";
    public static final String INTERNAL_API_KEY_SCHEME = "internalApiKey";

    @Bean
    public OpenAPI courseServiceOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server()
                                .url("https://dev-api.studybytes.ru/course-service")
                                .description("Development gateway through Nginx"),
                        new Server()
                                .url("http://localhost:8082")
                                .description("Local direct access"),
                        new Server()
                                .url("http://course-service:8082")
                                .description("Docker backend network")
                ))
                .info(new Info()
                        .title("CourseService API")
                        .version("v1")
                        .description("Course content and structure service API."))
                .components(new Components()
                        .addSecuritySchemes(
                                BEARER_AUTH_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Bearer JWT for CourseService admin endpoints. Required roles: TEACHER or ADMIN.")
                        )
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
