package br.com.hahn.toxicbet.application.controller;

import br.com.hahn.toxicbet.infrastructure.service.JwtService;
import br.com.hahn.toxicbet.util.DateTimeConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Slf4j
public abstract class AbstractController {

    protected final JwtService jwtService;

    protected Mono<Void> updateOAuthUserApplicationWithLogging(String email) {
        return jwtService.updateOAuthUserApplication()
                .onErrorResume(e -> {
                    log.error("AbstractController: Continuing despite OAuth check failure for user email: {} at: {}", email, DateTimeConverter.formatInstantNow());
                    return Mono.empty();
                });
    }

    protected Mono<String> extractUserIdFromToken(ServerWebExchange exchange) {
        return Mono.defer(() -> exchange.getPrincipal()
                .cast(org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken.class)
                .map(jwtAuth -> {
                    var jwt = jwtAuth.getToken();
                    return jwt.getSubject();
                })
                .switchIfEmpty(Mono.error(new IllegalStateException("User ID not found in token")))
        );
    }
}