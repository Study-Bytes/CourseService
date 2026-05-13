package org.studyplatform.courseservice.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;
import org.studyplatform.courseservice.config.InternalApiProperties;
import org.studyplatform.courseservice.config.JwtSecurityProperties;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({InternalApiProperties.class, JwtSecurityProperties.class})
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            InternalApiKeyFilter internalApiKeyFilter,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(internalApiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/health",
                                "/ready",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml"
                        ).permitAll()
                        .requestMatchers(
                                "/api/v1/courses",
                                "/api/v1/courses/**",
                                "/api/v1/course-items/**"
                        ).permitAll()
                        .requestMatchers("/api/v1/internal/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasAnyAuthority("ROLE_TEACHER", "ROLE_ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                )
                .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter(JwtSecurityProperties properties) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> extractAuthorities(jwt, properties));
        return converter;
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt, JwtSecurityProperties properties) {
        Object rolesClaim = jwt.getClaim(properties.getRolesClaim());

        if (rolesClaim == null) {
            return List.of();
        }

        return extractRoles(rolesClaim).stream()
                .map(role -> normalizeRole(role, properties.getRolePrefix()))
                .filter(StringUtils::hasText)
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    private List<String> extractRoles(Object rolesClaim) {
        if (rolesClaim instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .toList();
        }

        if (rolesClaim instanceof String rolesString) {
            return Arrays.stream(rolesString.split("[,\\s]+"))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .toList();
        }

        return List.of(rolesClaim.toString().trim());
    }

    private String normalizeRole(String rawRole, String rolePrefix) {
        if (!StringUtils.hasText(rawRole)) {
            return rawRole;
        }

        String normalizedRole = rawRole.trim().toUpperCase(Locale.ROOT);
        String normalizedPrefix = StringUtils.hasText(rolePrefix) ? rolePrefix.trim().toUpperCase(Locale.ROOT) : "";

        if (StringUtils.hasText(normalizedPrefix) && !normalizedRole.startsWith(normalizedPrefix)) {
            return normalizedPrefix + normalizedRole;
        }

        return normalizedRole;
    }
}
