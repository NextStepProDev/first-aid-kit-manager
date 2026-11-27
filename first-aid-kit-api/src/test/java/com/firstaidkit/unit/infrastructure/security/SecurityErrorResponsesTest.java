package com.firstaidkit.unit.infrastructure.security;

import com.firstaidkit.infrastructure.security.RateLimitingFilter;
import com.firstaidkit.infrastructure.security.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Odpowiedzi 401, 403 i 429 serializuje ręcznie tworzony ObjectMapper, a nie mapper Springa.
 * Przy przejściu z Jacksona 2 na 3 zmienił się serializer, więc test pilnuje kształtu JSON-a:
 * status jako liczba, komunikat, ścieżka i znacznik czasu ISO-8601. Frontend pokazuje `message`
 * z 429 na stronach rejestracji i resetu hasła (RegisterPage, ResetPasswordPage); na 401 i 403
 * reaguje samym kodem HTTP.
 */
class SecurityErrorResponsesTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    // Filtry są potrzebne tylko do łańcucha zabezpieczeń, a ten test woła same handlery.
    private final SecurityConfiguration securityConfiguration = new SecurityConfiguration(null, null, null);

    @Test
    void shouldWrite401BodyFromAuthenticationEntryPoint() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/drugs/search");
        MockHttpServletResponse response = new MockHttpServletResponse();

        securityConfiguration.authenticationEntryPoint()
                .commence(request, response, new InsufficientAuthenticationException("no token"));

        assertErrorBody(response, 401,
                "Authentication required. Please provide a valid JWT token in the Authorization header.",
                "/api/drugs/search");
    }

    @Test
    void shouldWrite403BodyFromAccessDeniedHandler() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/users");
        MockHttpServletResponse response = new MockHttpServletResponse();

        securityConfiguration.accessDeniedHandler()
                .handle(request, response, new AccessDeniedException("not admin"));

        assertErrorBody(response, 403,
                "Access denied. You don't have permission to access this resource.",
                "/api/admin/users");
    }

    @Test
    void shouldWrite429BodyWhenRateLimitIsExceeded() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter();
        ReflectionTestUtils.setField(filter, "rateLimitEnabled", true);
        ReflectionTestUtils.setField(filter, "requestsPerMinute", 1);

        MockHttpServletResponse first = new MockHttpServletResponse();
        filter.doFilter(loginRequest(), first, new MockFilterChain());
        assertThat(first.getStatus()).isEqualTo(200);

        MockHttpServletResponse second = new MockHttpServletResponse();
        filter.doFilter(loginRequest(), second, new MockFilterChain());

        assertErrorBody(second, 429, "Too many requests. Please try again later.", "/api/auth/login");
    }

    private static MockHttpServletRequest loginRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("203.0.113.7");
        return request;
    }

    private static void assertErrorBody(MockHttpServletResponse response, int status, String message, String path)
            throws Exception {
        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);

        JsonNode body = JSON.readTree(response.getContentAsString());
        assertThat(body.propertyNames()).containsExactlyInAnyOrder("status", "message", "timestamp", "path");
        assertThat(body.get("status").isInt()).as("status must be a JSON number").isTrue();
        assertThat(body.get("status").intValue()).isEqualTo(status);
        assertThat(body.get("message").asString()).isEqualTo(message);
        assertThat(body.get("path").asString()).isEqualTo(path);
        String timestamp = body.get("timestamp").asString();
        assertThatCode(() -> OffsetDateTime.parse(timestamp)).doesNotThrowAnyException();
    }
}
