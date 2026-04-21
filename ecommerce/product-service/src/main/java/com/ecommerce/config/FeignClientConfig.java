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

    @Bean
    public RequestInterceptor headerPropagationInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                var req = attrs.getRequest();
                String[] headers = {"X-User-Id", "X-User-Name", "X-User-Role", "X-B3-TraceId", "X-B3-SpanId"};
                for (String h : headers) {
                    String val = req.getHeader(h);
                    if (val != null) requestTemplate.header(h, val);
                }
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
            log.error("Feign error: {} status={}", methodKey, response.status());
            return new RuntimeException("Feign error " + response.status() + " on " + methodKey);
        };
    }
}
