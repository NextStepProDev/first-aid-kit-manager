package com.firstaidkit.integration.web;

import com.firstaidkit.integration.base.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.web.filter.ForwardedHeaderFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Backend stoi za nginx (first_aid_kit_frontend/nginx.conf), który ustawia X-Forwarded-For i
 * X-Forwarded-Proto. Spring bierze z nich schemat (np. linki https w Swagger UI) tylko wtedy, gdy
 * zarejestrowany jest ForwardedHeaderFilter, a o to prosi strategia "framework" w application.yml.
 * Boot 4.2 zmienił nazwę właściwości (server.forward-headers-strategy →
 * server.request.header.forwarded.strategy). Literówka albo kolejna zmiana nazwy po cichu usunęłaby filtr.
 */
class ForwardedHeaderStrategyTest extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void shouldRegisterForwardedHeaderFilterWhenFrameworkStrategyIsConfigured() {
        boolean registered = context.getBeansOfType(FilterRegistrationBean.class).values().stream()
                .anyMatch(bean -> bean.getFilter() instanceof ForwardedHeaderFilter);

        assertThat(registered)
                .as("ForwardedHeaderFilter is not registered: check the forwarded strategy in application.yml")
                .isTrue();
    }
}
