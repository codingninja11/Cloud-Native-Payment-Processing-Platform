package com.example.paymentplatform.order.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Component
@Profile("supabase")
public class SupabaseDbSanityCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SupabaseDbSanityCheck.class);

    private final DataSource dataSource;

    public SupabaseDbSanityCheck(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection c = dataSource.getConnection();
             Statement st = c.createStatement();
             var rs = st.executeQuery("select current_database(), current_user")) {
            if (rs.next()) {
                log.info("Postgres session: database={} user={}", rs.getString(1), rs.getString(2));
            }
        }
    }
}
