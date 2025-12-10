package com.cotic.backend.usersapp.backend_usersapp.auth;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import com.cotic.backend.usersapp.backend_usersapp.auth.filters.JwtAuthenticationFilter;
import com.cotic.backend.usersapp.backend_usersapp.auth.filters.JwtValidationFilter;

import jakarta.validation.constraints.NotNull;

@Configuration
public class SpringSecurityConfig {

    @Autowired
    private AuthenticationConfiguration authenticationConfiguration;

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager() throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain securityFilterChain(@NotNull HttpSecurity security) throws Exception {
        return security.authorizeHttpRequests((authorizeRequests) -> authorizeRequests
                // Allow access to OpenAPI / Swagger UI endpoints
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/swagger-ui-custom.html", "/api-docs/**",
                        "/v3/api-docs/**", "/webjars/**")
                .permitAll()
                // Allow public access to token validation endpoint
                .requestMatchers(HttpMethod.POST, "/users/validate-token").permitAll()
                .requestMatchers(HttpMethod.GET, "/users").permitAll()
                .requestMatchers(HttpMethod.GET, "/users/{id}").hasAnyRole("USER", "ADMIN")// se omite la palabra
                                                                                           // "ROLE_"
                .requestMatchers(HttpMethod.POST, "/users/").hasRole("ADMIN")
                .requestMatchers("/users/**").hasRole("ADMIN")// acceso a cualquier ruta para el role ADMIN
                // .requestMatchers(HttpMethod.DELETE, "/users/{id}").hasRole("ADMIN")
                // .requestMatchers(HttpMethod.PUT, "/users/{id}").hasRole("ADMIN")
                .requestMatchers("/h2-console/**").permitAll()
                .anyRequest().authenticated())
                .addFilter(new JwtAuthenticationFilter(authenticationConfiguration.getAuthenticationManager()))
                .addFilter(new JwtValidationFilter(authenticationConfiguration.getAuthenticationManager()))
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions().disable())
                .sessionManagement(
                        (sessionManagement) -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
        config.setAllowedOriginPatterns(Arrays.asList("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    FilterRegistrationBean<CorsFilter> corsFilter() {
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(
                new CorsFilter(corsConfigurationSource()));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
