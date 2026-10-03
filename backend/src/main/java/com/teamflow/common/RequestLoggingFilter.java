package com.teamflow.common;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId); response.setHeader("X-Request-ID", requestId);
        long start = System.nanoTime();
        try { chain.doFilter(request, response); }
        finally {
            // Deliberately exclude credentials, tokens, query strings and request bodies.
            log.info("HTTP method={} status={} durationMs={}", request.getMethod(), response.getStatus(), (System.nanoTime()-start)/1_000_000);
            MDC.remove("requestId");
        }
    }
}
