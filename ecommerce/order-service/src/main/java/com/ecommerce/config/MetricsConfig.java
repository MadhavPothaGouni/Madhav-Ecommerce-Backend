package com.ecommerce.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * Tag every metric with the service name so Grafana can filter by service.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
                .commonTags("application", applicationName)
                .commonTags("environment", System.getProperty("spring.profiles.active", "local"));
    }

    /**
     * Custom timer for business operations — example: order creation latency.
     * Inject and use in services: timer.record(() -> doSomething()).
     */
    @Bean
    public Timer orderCreationTimer(MeterRegistry registry) {
        return Timer.builder("ecommerce.order.creation.duration")
                .description("Time taken to create an order including Feign calls")
                .publishPercentileHistogram()
                .register(registry);
    }
}
