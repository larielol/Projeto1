package com.vitral.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import com.vitral.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    private static final String[] PUBLIC_PATHS = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/confirmar",
            "/api/v1/auth/recuperar-senha",
            "/api/v1/auth/redefinir-senha",
            "/api/v1/auth/reenviar-confirmacao",
            "/error",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/home").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/ofertas/minhas").hasRole("SEBO")
                        .requestMatchers(HttpMethod.GET, "/api/v1/sebos/**", "/api/v1/produtos/**", "/api/v1/busca/**",
                                "/api/v1/categorias/**", "/api/v1/ofertas/**", "/api/v1/uploads/images/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/sebos/me").hasRole("SEBO")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/usuarios/me").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/ofertas").hasRole("SEBO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/produtos").hasRole("SEBO")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/produtos/**").hasRole("SEBO")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/produtos/**").hasRole("SEBO")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/ofertas/**").hasRole("SEBO")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/ofertas/**").hasRole("SEBO")
                        .requestMatchers("/api/v1/mensagens/**").authenticated()
                        .requestMatchers("/api/v1/cesta/**").hasRole("USUARIO")
                        .requestMatchers("/api/v1/favoritos/**").hasRole("USUARIO")
                        .requestMatchers("/api/v1/recomendacoes/**").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/pedidos").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/pedidos/*/cancelar").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.GET, "/api/v1/pedidos/meus-pedidos").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/pedidos/*/status").hasRole("SEBO")
                        .requestMatchers(HttpMethod.GET, "/api/v1/pedidos/vendas").hasRole("SEBO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/suporte").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
