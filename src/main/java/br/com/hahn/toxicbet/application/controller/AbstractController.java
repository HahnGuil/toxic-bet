package br.com.hahn.toxicbet.application.controller;

import br.com.hahn.toxicbet.infrastructure.service.JwtService;
import br.com.hahn.toxicbet.util.DateTimeConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Slf4j
public abstract class AbstractController {

    protected final JwtService jwtService;

    protected Mono<Void> updateOAuthUserApplication(String email) {
        return jwtService.updateOAuthUserApplication()
                .onErrorResume(e -> {
                    log.error("AbstractController: Continuing despite OAuth check failure for user email: {} at: {}", email, DateTimeConverter.formatInstantNow());
                    return Mono.empty();
                });
    }

    protected Mono<String> extractUserEmailFromToken(ServerWebExchange exchange) {
        return Mono.defer(() -> exchange.getPrincipal()
                .cast(JwtAuthenticationToken.class)
                .map(JwtAuthenticationToken::getToken)
                .flatMap(jwt -> Mono.justOrEmpty(jwt.getSubject()))
                .filter(email -> !email.isBlank())
                .switchIfEmpty(Mono.error(new IllegalStateException("User email not found in token")))
                .doOnError(error -> log.error(
                        "AbstractController: Failed to extract user email from token. path={}, at={}, reason={}",
                        exchange.getRequest().getPath().value(),
                        DateTimeConverter.formatInstantNow(),
                        error.getMessage(),
                        error
                )));
    }

}