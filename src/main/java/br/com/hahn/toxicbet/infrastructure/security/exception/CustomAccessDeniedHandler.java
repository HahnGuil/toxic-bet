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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAccessDeniedHandler implements ServerAccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /**
     * Handles an access denied exception by sending a JSON response with an error message and timestamp.
     *
     * @author HahnGuil
     * @param exchange the server web exchange
     * @param denied the exception that was thrown due to access being denied
     * @return a Mono that completes when the response has been written
     */
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
        log.error("CustomAccessDeniedHandler: Access denied: {} at: {}", denied.getMessage(), DateTimeConverter.formatInstantNow());

        return Mono.fromCallable(() -> {
                    ErrorResponseDTO errorResponse = new ErrorResponseDTO()
                            .message("You don't have permission to access this operation")
                            .timestamp(OffsetDateTime.now());

                    objectMapper.registerModule(new JavaTimeModule());
                    return objectMapper.writeValueAsBytes(errorResponse);
                })
                .flatMap(bytes -> {
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

                    DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
                    return exchange.getResponse().writeWith(Mono.just(buffer));
                })
                .onErrorResume(e -> {
                    log.error("CustomAccessDeniedHandler: Error writing access denied response: {}, at: {}", e, DateTimeConverter.formatInstantNow());
                    return Mono.empty();
                });
    }
}