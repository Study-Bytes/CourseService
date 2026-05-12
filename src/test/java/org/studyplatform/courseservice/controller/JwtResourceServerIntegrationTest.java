package org.studyplatform.courseservice.controller;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.studyplatform.courseservice.repository.CourseItemContentBlockRepository;
import org.studyplatform.courseservice.repository.CourseItemHintRepository;
import org.studyplatform.courseservice.repository.CourseItemOptionRepository;
import org.studyplatform.courseservice.repository.CourseItemRepository;
import org.studyplatform.courseservice.repository.CourseItemTestCaseRepository;
import org.studyplatform.courseservice.repository.CourseModuleRepository;
import org.studyplatform.courseservice.repository.CourseRepository;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = JwtResourceServerIntegrationTest.JwtPropertiesInitializer.class)
class JwtResourceServerIntegrationTest {

    private static final String ISSUER = "http://user-service-test:8081";
    private static final String AUDIENCE = "study-platform";
    private static final RSAKey RSA_KEY = createRsaKey();
    private static final HttpServer JWK_SERVER = startJwkServer();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseItemContentBlockRepository contentBlockRepository;

    @Autowired
    private CourseItemHintRepository hintRepository;

    @Autowired
    private CourseItemOptionRepository optionRepository;

    @Autowired
    private CourseItemTestCaseRepository testCaseRepository;

    @Autowired
    private CourseItemRepository itemRepository;

    @Autowired
    private CourseModuleRepository moduleRepository;

    @Autowired
    private CourseRepository courseRepository;

    @BeforeEach
    void cleanDatabase() {
        contentBlockRepository.deleteAll();
        hintRepository.deleteAll();
        optionRepository.deleteAll();
        testCaseRepository.deleteAll();
        itemRepository.deleteAll();
        moduleRepository.deleteAll();
        courseRepository.deleteAll();
    }

    @AfterAll
    static void stopJwkServer() {
        JWK_SERVER.stop(0);
    }

    @Test
    void shouldAcceptJwtSignedByUserServiceJwks() throws Exception {
        String token = createJwt("1", List.of("TEACHER"));

        mockMvc.perform(post("/api/v1/admin/courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createCourseRequest("jwks-teacher", 1L)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdByUserId").value(1));
    }

    @Test
    void shouldApplyOwnershipChecksForJwksVerifiedJwt() throws Exception {
        String token = createJwt("2", List.of("TEACHER"));

        mockMvc.perform(post("/api/v1/admin/courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createCourseRequest("jwks-foreign-owner", 1L)))
                .andExpect(status().isForbidden());
    }

    private static String createCourseRequest(String slugPrefix, Long createdByUserId) {
        return """
                {
                  "slug": "%s-%s",
                  "title": "JWKS Test Course",
                  "createdByUserId": %d
                }
                """.formatted(slugPrefix, System.nanoTime(), createdByUserId);
    }

    private static String createJwt(String subject, List<String> roles) throws JOSEException {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject(subject)
                .audience(AUDIENCE)
                .claim("roles", roles)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(3600)))
                .build();

        SignedJWT signedJwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(RSA_KEY.getKeyID())
                        .build(),
                claims
        );
        signedJwt.sign(new RSASSASigner(RSA_KEY));
        return signedJwt.serialize();
    }

    private static RSAKey createRsaKey() {
        try {
            return new RSAKeyGenerator(2048)
                    .keyID("course-service-test-key")
                    .generate();
        } catch (JOSEException e) {
            throw new IllegalStateException("Unable to generate test RSA key", e);
        }
    }

    private static HttpServer startJwkServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/v1/auth/.well-known/jwks.json", exchange -> {
                byte[] response = ("{\"keys\":[" + RSA_KEY.toPublicJWK().toJSONString() + "]}").getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to start test JWKS server", e);
        }
    }

    static class JwtPropertiesInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues.of(
                    "spring.security.oauth2.resourceserver.jwt.issuer-uri=" + ISSUER,
                    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:"
                            + JWK_SERVER.getAddress().getPort()
                            + "/api/v1/auth/.well-known/jwks.json",
                    "spring.security.oauth2.resourceserver.jwt.audiences=" + AUDIENCE
            ).applyTo(applicationContext.getEnvironment());
        }
    }
}
