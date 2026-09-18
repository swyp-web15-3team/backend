package com.team3.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team3.user.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
        @Value("${springdoc.api-docs.enabled:false}") boolean docsEnabled, ObjectMapper objectMapper,
        UserRepository users)
        throws Exception {
        AuthEntryPoint authenticationEntryPoint = new AuthEntryPoint(objectMapper);
        http.csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers.referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER)))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers(HttpMethod.POST, "/api/v1/auth/kakao", "/api/v1/auth/refresh",
                    "/api/v1/auth/logout").permitAll();
                auth.requestMatchers(HttpMethod.GET, "/api/v1/whisky-categories", "/api/v1/whiskies",
                    "/api/v1/whiskies/suggestions", "/api/v1/whiskies/{whiskyId}")
                    .permitAll();
                auth.requestMatchers("/actuator/health", "/error").permitAll();
                if (docsEnabled) {
                    auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                }
                auth.requestMatchers(HttpMethod.POST, "/api/v1/auth/sign-up").authenticated();
                auth.requestMatchers(HttpMethod.DELETE, "/api/v1/auth/withdrawal").authenticated();
                auth.anyRequest().access(
                    (authentication, context) -> new AuthorizationDecision(isActiveUser(authentication.get(), users)));
            })
            .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults())
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(new AuthAccessDeniedHandler(objectMapper)));
        return http.build();
    }

    private boolean isActiveUser(Authentication authentication, UserRepository users) {
        if (!(authentication instanceof JwtAuthenticationToken jwt) || !authentication.isAuthenticated()) {
            return false;
        }
        try {
            long userId = Long.parseLong(jwt.getToken().getSubject());
            return userId > 0 && users.findById(userId)
                .filter(user -> user.isActive() && !user.isDeleted()).isPresent();
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
