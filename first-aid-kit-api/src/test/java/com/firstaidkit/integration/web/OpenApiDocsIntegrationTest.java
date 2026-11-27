package com.firstaidkit.integration.web;

import com.firstaidkit.integration.base.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * springdoc 3.2.1 działa na swagger-core 3.0 i Jacksonie 3, a swagger-core prosi o nowszego
 * Jacksona (3.2.x), niż daje BOM Spring Boota (3.1.x). Rozjazd wersji wyszedłby dopiero przy
 * budowaniu dokumentacji, więc test generuje ją całą na podstawie prawdziwych kontrolerów.
 */
class OpenApiDocsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldGenerateOpenApiDocsForTheApi() {
        ResponseEntity<String> response = restTemplate.getForEntity("/v3/api-docs", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("\"openapi\"")
                .contains("/api/drugs/search")
                .contains("/api/auth/login");
    }
}
