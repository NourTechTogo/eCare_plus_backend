package com.nourtech.ecareplus.auth.infrastructure.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.time.Clock;
import java.util.List;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    JwtService jwtService(JwtProperties properties) {
        return new JwtService(properties.secret(), properties.ttl(), Clock.systemUTC());
    }

    @Bean
    JwtDecoder jwtDecoder(JwtService jwtService, com.nourtech.ecareplus.auth.application.service.SessionService sessions) {
        JwtDecoder delegate = jwtService.decoder();
        return token -> { var jwt = delegate.decode(token); sessions.verifier(jwt); return jwt; };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            return role == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + role));
        });

        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/integrations/appels/livekit", "/api/v1/integrations/notifications/callback").permitAll()
                        .requestMatchers("/api/v1/auth/deconnexion", "/api/v1/compte/**", "/api/v1/compte").authenticated()
                        .requestMatchers("/api/v1/auth/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()
                        .requestMatchers("/api/v1/mobile/**").authenticated()
                        .requestMatchers("/api/v1/notes", "/api/v1/notes/**", "/api/v1/appels/**").authenticated()
                        .requestMatchers("/api/v1/**")
                        .hasAnyRole("ADMINISTRATEUR_STRUCTURE", "MEDECIN", "INFIRMIER")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .bearerTokenResolver(request -> request.getRequestURI().equals("/api/v1/integrations/appels/livekit") ? null : new org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver().resolve(request))
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint((request, response, exception) ->
                                ecrireProbleme(response, objectMapper, 401, "Authentification requise",
                                        "Un jeton valide est nécessaire")))
                .exceptionHandling(errors -> errors.accessDeniedHandler((request, response, exception) ->
                        ecrireProbleme(response, objectMapper, 403, "Accès refusé",
                                "Vous n'êtes pas autorisé à effectuer cette opération")))
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
            List<String> allowedOriginPatterns
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(allowedOriginPatterns);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    private static void ecrireProbleme(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            int statut,
            String titre,
            String detail
    ) throws IOException {
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(statut), detail);
        probleme.setTitle(titre);
        probleme.setProperty("erreurs", List.of());
        response.setStatus(statut);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), probleme);
    }
}
