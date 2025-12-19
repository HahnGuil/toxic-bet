package br.com.hahn.toxicbet.infrastructure.security;

import io.netty.channel.ChannelOption;
import lombok. extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter. Converter;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework. security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt. Jwt;
import org.springframework. security.oauth2.jwt.JwtTimestampValidator;
import org. springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org. springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security. oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication. ReactiveJwtAuthenticationConverterAdapter;
import org.springframework. security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
@EnableCaching
@Slf4j
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-cache-lifespan:3600000}")
    private long jwkCacheLifespan;

    @Value("${spring.security.oauth2.resourceserver.jwt.connect-timeout:30000}")
    private int connectTimeout;

    @Value("${spring.security.oauth2.resourceserver.jwt.read-timeout:30000}")
    private int readTimeout;

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        return http
                . csrf(ServerHttpSecurity.CsrfSpec::disable)
                . securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .authorizeExchange(auth -> auth
                        .pathMatchers("/actuator/**", "/public/**").permitAll()
                        .pathMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtDecoder(customJwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )
                .build();
    }

    @Bean
    public ReactiveJwtDecoder customJwtDecoder() {
        try {
            WebClient webClient = WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(
                            HttpClient.create()
                                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                                    .responseTimeout(Duration.ofMillis(readTimeout))
                    ))
                    .build();

            NimbusReactiveJwtDecoder jwtDecoder = NimbusReactiveJwtDecoder
                    .withJwkSetUri(jwkSetUri)
                    .webClient(webClient)
                    .build();

            jwtDecoder.setJwtValidator(new JwtTimestampValidator());

            return jwtDecoder;
        } catch (Exception e) {
            log.error("Error creating JWT Decoder", e);
//            TODO - TROCAR POR UMA EXCEÇÃO PERSONALLIZADA
            throw new RuntimeException("Failed to configure JWT Decoder", e);
        }
    }

    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter. setJwtGrantedAuthoritiesConverter(jwt -> {
            String scope = jwt.getClaimAsString("scope");
            if (scope != null && !scope.isEmpty()) {
                return List.of(new SimpleGrantedAuthority("SCOPE_" + scope));
            }
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        });

        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }
}