package com.eunhyehymn.common.config;

import com.eunhyehymn.common.error.ApiAccessDeniedHandler;
import com.eunhyehymn.common.error.ApiAuthenticationEntryPoint;
import com.eunhyehymn.infrastructure.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        JwtAuthenticationFilter jwtAuthenticationFilter,
        ApiAuthenticationEntryPoint apiAuthenticationEntryPoint,
        ApiAccessDeniedHandler apiAccessDeniedHandler
    )
        throws Exception {
        // Why: default-deny and opt-in per path until auth flows are complete.
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/ping", "/actuator/health").permitAll()
                .requestMatchers("/auth/sms/**", "/auth/withdraw").authenticated()
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/hymns").permitAll()
                .requestMatchers("/hymns/**").authenticated()
                .requestMatchers("/ai/**").authenticated()
                .requestMatchers("/events").authenticated()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/me/**").authenticated()
                .anyRequest().denyAll()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(apiAuthenticationEntryPoint)
                .accessDeniedHandler(apiAccessDeniedHandler)
            )
            .build();
    }
}
