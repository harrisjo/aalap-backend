package com.aalap.aalapbackend.security;

import com.aalap.aalapbackend.exception.NullUserException;
import com.aalap.aalapbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {
    JwtFilter jwtFilter;
    UserRepository userRepository;
    AuthRateLimitFilter authRateLimitFilter;

    @Autowired
    public SecurityConfig(JwtFilter jwtFilter,
                          UserRepository userRepository,
                          AuthRateLimitFilter authRateLimitFilter) {
        this.jwtFilter = jwtFilter;
        this.userRepository = userRepository;
        this.authRateLimitFilter = authRateLimitFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ── Security response headers ──────────────────────────────────────────
                // Spring Security defaults already add: X-Content-Type-Options, X-Frame-Options,
                // X-XSS-Protection, Cache-Control, and HSTS (on HTTPS, 1 year + includeSubDomains).
                // We explicitly configure the two policies NOT set by default.
                // Statement style (not method-chaining) is required for Spring Security 7
                // because some configurers return their own sub-type, not HeadersConfigurer.
                .headers(headers -> {
                    headers.frameOptions(frame -> frame.deny());
                    headers.referrerPolicy(referrer -> referrer
                            .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                    // permissionsPolicy(Customizer) was removed in Spring Security 7 —
                    // use StaticHeadersWriter to set the Permissions-Policy header directly.
                    headers.addHeaderWriter(new StaticHeadersWriter(
                            "Permissions-Policy",
                            "camera=(), microphone=(), geolocation=(), payment=()"));
                })
                // ── CSRF ──────────────────────────────────────────────────────────
                // Now using the double-submit cookie pattern (CookieCsrfTokenRepository).
                // Spring Security writes XSRF-TOKEN (non-HttpOnly) on every response;
                // the SPA reads it and echoes it back as X-XSRF-TOKEN header.
                // Auth endpoints are exempt — login/register have no prior session token.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers("/api/auth/**")
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/error").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(authRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> userRepository.findByEmail(email)
                .orElseThrow(() -> new NullUserException("User not found"));
    }

    @Bean
    public FilterRegistrationBean<JwtFilter> jwtFilterRegistration(JwtFilter jwtFilter) {
        FilterRegistrationBean<JwtFilter> registration = new FilterRegistrationBean<>(jwtFilter);
        registration.setEnabled(false);
        return registration;
    }

    /** Prevent AuthRateLimitFilter from being registered twice (once by Spring Boot auto-config, once in the security chain). */
    @Bean
    public FilterRegistrationBean<AuthRateLimitFilter> rateLimitFilterRegistration(AuthRateLimitFilter filter) {
        FilterRegistrationBean<AuthRateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173", "https://aalapmusic.vercel.app"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true); // Required for cookies to be sent cross-origin
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
