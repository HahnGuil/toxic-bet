package br.com.hahn.toxicbet.infrastructure.security.exception;

import br.com.hahn.toxicbet.model.ErrorResponseDTO;
import br.com.hahn.toxicbet.util.DateTimeConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

/**
 * Custom implementation of {@link ServerAuthenticationEntryPoint} to handle authentication errors
 * in a reactive Spring Security application.
 * <p>
 * This class sends a JSON response with an error message and timestamp when authentication fails.
 * </p>
 *
 * @author HahnGuil
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAuthenticationEntryPointHandler implements ServerAuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * Handles authentication exceptions by sending a JSON response with an error message and timestamp.
     *
     * @param exchange the {@link ServerWebExchange} that triggered the authentication exception
     * @param ex the {@link AuthenticationException} that was thrown due to authentication failure
     * @return a {@link Mono} that completes when the response has been written
     */
    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        log.error("CustomAuthenticationEntryPointHandler: Authentication error: {} at: {}", ex.getMessage(), DateTimeConverter.formatInstantNow());

        return Mono.fromCallable(() -> {
                    ErrorResponseDTO errorResponse = new ErrorResponseDTO()
                            .message("User not authenticated. Please log in to continue")
                            .timestamp(OffsetDateTime.now());

                    objectMapper.registerModule(new JavaTimeModule());
                    return objectMapper.writeValueAsBytes(errorResponse);
                })
                .flatMap(bytes -> {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

                    DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
                    return exchange.getResponse().writeWith(Mono.just(buffer));
                })
                .onErrorResume(e -> {
                    log.error("CustomAuthenticationEntryPointHandler: Error writing authentication error response: {}, at {}", e, DateTimeConverter.formatInstantNow());
                    return Mono.empty();
                });
    }
}