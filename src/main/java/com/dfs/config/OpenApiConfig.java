package com.dfs.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI distributedFileStorageOpenApi(AppProperties appProperties) {
        return new OpenAPI().info(new Info()
                .title("Distributed File Storage System API")
                .version(appProperties.version())
                .description("REST API for the Distributed File Storage System. "
                        + "Phase 1 exposes health, configuration and error handling."));
    }
}
