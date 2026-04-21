package com.ecommerce.config;

import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Configuration
public class FeignClientConfig {

    /**
     * Propagate security headers (X-User-*) and trace headers to downstream services.
     */
    @Bean
    public RequestInterceptor headerPropagationInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                var request = attrs.getRequest();
                String userId = request.getHeader("X-User-Id");
                String userName = request.getHeader("X-User-Name");
                String userRole = request.getHeader("X-User-Role");
                String traceId = request.getHeader("X-B3-TraceId");
                String spanId = request.getHeader("X-B3-SpanId");

                if (userId != null)   requestTemplate.header("X-User-Id", userId);
                if (userName != null) requestTemplate.header("X-User-Name", userName);
                if (userRole != null) requestTemplate.header("X-User-Role", userRole);
                if (traceId != null)  requestTemplate.header("X-B3-TraceId", traceId);
                if (spanId != null)   requestTemplate.header("X-B3-SpanId", spanId);

                log.debug("Feign propagating headers - userId: {}, traceId: {}", userId, traceId);
            }
        };
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public ErrorDecoder feignErrorDecoder() {
        return (methodKey, response) -> {
            log.error("Feign error: method={} status={}", methodKey, response.status());
            if (response.status() == 404) {
                return new RuntimeException("Resource not found via Feign: " + methodKey);
            }
            if (response.status() == 409) {
                return new RuntimeException("Conflict error via Feign: " + methodKey);
            }
            return new RuntimeException("Feign client error: " + response.status() + " on " + methodKey);
        };
    }
}
