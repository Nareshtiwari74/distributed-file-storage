package com.dfs.health;

import com.dfs.config.AppProperties;
import com.dfs.health.dto.HealthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthServiceTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final AppProperties appProperties = new AppProperties("DFS", "0.1.0-test");
    private final HealthService healthService = new HealthService(jdbcTemplate, appProperties);

    @Test
    void reportsUpWhenDatabaseAnswers() {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);

        HealthResponse response = healthService.check();

        assertThat(response.status()).isEqualTo("UP");
        assertThat(response.components()).containsEntry("database", "UP");
        assertThat(response.application()).isEqualTo("DFS");
        assertThat(response.version()).isEqualTo("0.1.0-test");
    }

    @Test
    void reportsDownWhenDatabaseUnreachable() {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
                .thenThrow(new DataAccessResourceFailureException("connection refused"));

        HealthResponse response = healthService.check();

        assertThat(response.status()).isEqualTo("DOWN");
        assertThat(response.components()).containsEntry("database", "DOWN");
    }
}
