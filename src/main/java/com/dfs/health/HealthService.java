package com.dfs.health;

import com.dfs.config.AppProperties;
import com.dfs.health.dto.HealthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class HealthService {

    private static final Logger log = LoggerFactory.getLogger(HealthService.class);
    private static final String STATUS_UP = "UP";
    private static final String STATUS_DOWN = "DOWN";

    private final JdbcTemplate jdbcTemplate;
    private final AppProperties appProperties;

    public HealthService(JdbcTemplate jdbcTemplate, AppProperties appProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.appProperties = appProperties;
    }

    public HealthResponse check() {
        Map<String, String> components = new LinkedHashMap<>();
        boolean databaseUp = isDatabaseReachable();
        components.put("database", databaseUp ? STATUS_UP : STATUS_DOWN);

        String aggregate = databaseUp ? STATUS_UP : STATUS_DOWN;
        return new HealthResponse(
                aggregate,
                appProperties.name(),
                appProperties.version(),
                components,
                Instant.now());
    }

    private boolean isDatabaseReachable() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return result != null && result == 1;
        } catch (DataAccessException ex) {
            log.warn("Database health probe failed: {}", ex.getMessage());
            return false;
        }
    }
}
