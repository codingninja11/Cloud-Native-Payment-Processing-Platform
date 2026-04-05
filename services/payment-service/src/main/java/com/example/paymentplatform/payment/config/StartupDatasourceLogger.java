package com.example.paymentplatform.payment.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Logs active profiles and JDBC URL at startup so you can confirm data is going to Supabase vs localhost.
 */
@Component
public class StartupDatasourceLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupDatasourceLogger.class);

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    private final Environment environment;

    public StartupDatasourceLogger(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String[] active = environment.getActiveProfiles();
        String profiles = active.length == 0 ? "default" : Arrays.toString(active);
        log.info("Active profiles: {} | Datasource URL: {}", profiles, jdbcUrl);
    }
}
