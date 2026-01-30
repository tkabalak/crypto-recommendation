package com.example.crypto.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) configuration.
 *
 * <p>Registers ApiProblem schema and reusable error responses globally.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Base OpenAPI metadata.
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Crypto Recommendation Service")
                        .version("v1")
                        .description("Reads crypto prices from CSV (import at startup), stores in DB and exposes recommendation endpoints."));
    }


    /**
     * OpenAPI customizer that registers ApiProblem schema and reusable error responses.
     * <p>
     * Registers the ApiProblem schema and reusable Problem responses globally:
     * <ul>
     *   <li>components/schemas/ApiProblem</li>
     *   <li>components/responses/BadRequestProblem</li>
     *   <li>components/responses/NotFoundProblem</li>
     *   <li>components/responses/InternalServerErrorProblem</li>
     * </ul>
     *
     */
    @Bean
    public OpenApiCustomizer apiProblemOpenApiCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new io.swagger.v3.oas.models.Components());
            }

            // ApiProblem Schema
            Schema<?> schema = new Schema<>()
                    .name("ApiProblem")
                    .type("object")
                    .description("RFC7807 Problem Details (application/problem+json)")
                    .addProperty("type", new Schema<>().type("string").format("uri"))
                    .addProperty("title", new Schema<>().type("string"))
                    .addProperty("status", new Schema<>().type("integer").format("int32"))
                    .addProperty("detail", new Schema<>().type("string"))
                    .addProperty("instance", new Schema<>().type("string"))
                    .addProperty("symbol", new Schema<>().type("string"))
                    .addProperty("violations", new Schema<>().type("array").items(new Schema<>().type("string")))
                    .addProperty("clientIp", new Schema<>().type("string"));

            openApi.getComponents().addSchemas("ApiProblem", schema);

            // Reusable responses
            var problemMediaType = new io.swagger.v3.oas.models.media.MediaType()
                    .schema(new Schema<>().$ref("#/components/schemas/ApiProblem"));

            var problemContent = new io.swagger.v3.oas.models.media.Content()
                    .addMediaType("application/problem+json", problemMediaType);

            openApi.getComponents().addResponses("BadRequestProblem",
                    new io.swagger.v3.oas.models.responses.ApiResponse()
                            .description("Bad request")
                            .content(problemContent));

            openApi.getComponents().addResponses("NotFoundProblem",
                    new io.swagger.v3.oas.models.responses.ApiResponse()
                            .description("Not found")
                            .content(problemContent));


            openApi.getComponents().addResponses("TooManyRequestsProblem",
                    new io.swagger.v3.oas.models.responses.ApiResponse()
                            .description("Too many requests")
                            .content(problemContent));

            openApi.getComponents().addResponses("InternalServerErrorProblem",
                    new io.swagger.v3.oas.models.responses.ApiResponse()
                            .description("Internal server error")
                            .content(problemContent));

            // Attach globally to all operations
            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().values().forEach(pathItem -> {
                pathItem.readOperations().forEach(op -> {
                    if (op.getResponses() == null) {
                        op.setResponses(new io.swagger.v3.oas.models.responses.ApiResponses());
                    }
                    // 400
                    op.getResponses().putIfAbsent("400",
                            new io.swagger.v3.oas.models.responses.ApiResponse().$ref("#/components/responses/BadRequestProblem"));
                    // 404
                    op.getResponses().putIfAbsent("404",
                            new io.swagger.v3.oas.models.responses.ApiResponse().$ref("#/components/responses/NotFoundProblem"));
                    // 429
                    op.getResponses().putIfAbsent("429",
                            new io.swagger.v3.oas.models.responses.ApiResponse().$ref("#/components/responses/TooManyRequestsProblem"));
                    // 500
                    op.getResponses().putIfAbsent("500",
                            new io.swagger.v3.oas.models.responses.ApiResponse().$ref("#/components/responses/InternalServerErrorProblem"));
                });
            });
        };
    }

}
