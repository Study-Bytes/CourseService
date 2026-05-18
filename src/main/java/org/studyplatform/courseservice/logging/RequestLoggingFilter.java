package org.studyplatform.courseservice.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String REQUEST_ID_MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        long startedAt = System.nanoTime();

        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        String method = request.getMethod();
        String path = pathWithQuery(request);
        String remoteAddress = request.getRemoteAddr();
        boolean failed = false;

        log.info("HTTP request started method={} path={} remoteAddress={}", method, path, remoteAddress);

        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException exception) {
            failed = true;
            log.error(
                    "HTTP request failed method={} path={} status={} durationMs={} remoteAddress={}",
                    method,
                    path,
                    effectiveFailureStatus(response),
                    elapsedMillis(startedAt),
                    remoteAddress,
                    exception
            );
            throw exception;
        } finally {
            int status = failed ? effectiveFailureStatus(response) : response.getStatus();
            long durationMs = elapsedMillis(startedAt);

            if (status >= 500) {
                log.error("HTTP request completed method={} path={} status={} durationMs={} remoteAddress={}",
                        method, path, status, durationMs, remoteAddress);
            } else if (status >= 400) {
                log.warn("HTTP request completed method={} path={} status={} durationMs={} remoteAddress={}",
                        method, path, status, durationMs, remoteAddress);
            } else {
                log.info("HTTP request completed method={} path={} status={} durationMs={} remoteAddress={}",
                        method, path, status, durationMs, remoteAddress);
            }

            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (StringUtils.hasText(requestId)) {
            return requestId.trim();
        }

        return UUID.randomUUID().toString();
    }

    private String pathWithQuery(HttpServletRequest request) {
        String path = request.getRequestURI();
        String query = request.getQueryString();

        if (!StringUtils.hasText(query)) {
            return path;
        }

        return path + "?" + query;
    }

    private long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private int effectiveFailureStatus(HttpServletResponse response) {
        int status = response.getStatus();

        if (status >= HttpServletResponse.SC_BAD_REQUEST) {
            return status;
        }

        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }
}
