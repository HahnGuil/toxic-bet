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
                .doOnSubscribe(s -> log.debug("AbstractController: Checking OAuth user status for user email:  {}, at: {}", email, DateTimeConverter.formatInstantNow()))
                .doOnSuccess(v -> log.debug("AbstractController: OAuth check completed for user email: {}, at: {}", email, DateTimeConverter.formatInstantNow()))
                . doOnError(err -> log.error("AbstractController: Error checking OAuth status for user email: {} at: {}", email, DateTimeConverter.formatInstantNow()))
                .onErrorResume(e -> {
                    log.warn("AbstractController: Continuing despite OAuth check failure for user email: {} at: {}", email, DateTimeConverter.formatInstantNow());
                    return Mono.empty();
                });
    }

    protected Mono<String> extractUserIdFromToken(ServerWebExchange exchange) {
        return Mono.defer(() -> exchange.getPrincipal()
                .cast(org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken.class)
                .map(jwtAuth -> {
                    var jwt = jwtAuth.getToken();
                    String userEmail = jwt.getSubject();
                    log.debug("AbstractController: Extracted user_id from token: {}", userEmail);
                    return userEmail;
                })
                .switchIfEmpty(Mono.error(new IllegalStateException("User ID not found in token")))
        );
    }
}