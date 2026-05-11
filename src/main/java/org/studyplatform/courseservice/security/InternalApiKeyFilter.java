package org.studyplatform.courseservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.studyplatform.courseservice.config.InternalApiProperties;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;

@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Internal-Api-Key";

    private final InternalApiProperties internalApiProperties;

    public InternalApiKeyFilter(InternalApiProperties internalApiProperties) {
        this.internalApiProperties = internalApiProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/internal/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String expectedApiKey = internalApiProperties.getInternalApiKey();
        String actualApiKey = request.getHeader(HEADER_NAME);

        if (!StringUtils.hasText(expectedApiKey) || !Objects.equals(expectedApiKey, actualApiKey)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                    {"status":401,"error":"Unauthorized","message":"Invalid or missing internal API key","timestamp":"%s"}
                    """.formatted(Instant.now()));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
