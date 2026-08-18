package com.dfs.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI distributedFileStorageOpenApi(AppProperties appProperties) {
        return new OpenAPI()
                .info(new Info()
                        .title("Distributed File Storage System API")
                        .version(appProperties.version())
                        .description("REST API for the Distributed File Storage System — "
                                + "user authentication (JWT) and file upload, list, download, and delete. "
                                + "Click 'Authorize' and paste your JWT (from /api/auth/login) to call protected endpoints."))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
