package br.com.hahn.toxicbet.application.controller;

import br.com.hahn.toxicbet.application.service.PushNotificationService;
import br.com.hahn.toxicbet.application.service.PushNotificationService.PushSubscriptionDeleteRequest;
import br.com.hahn.toxicbet.application.service.PushNotificationService.PushSubscriptionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class PushNotificationController extends AbstractController {

    private final PushNotificationService pushNotificationService;

    @GetMapping("/notifications/public-key")
    public Mono<ResponseEntity<String>> getPublicKey() {
        return pushNotificationService.getPublicKey()
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NO_CONTENT).build());
    }

    @PostMapping("/notifications/subscriptions")
    public Mono<ResponseEntity<Void>> subscribe(
            @RequestBody Mono<PushSubscriptionRequest> request,
            ServerWebExchange exchange
    ) {
        String userAgent = exchange.getRequest().getHeaders().getFirst(HttpHeaders.USER_AGENT);
        return extractUserEmailFromToken(exchange)
                .flatMap(userEmail -> request.flatMap(body -> pushNotificationService.subscribe(body, userEmail, userAgent)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT).build());
    }

    @DeleteMapping("/notifications/subscriptions")
    public Mono<ResponseEntity<Void>> unsubscribe(@RequestBody Mono<PushSubscriptionDeleteRequest> request) {
        return request.flatMap(pushNotificationService::unsubscribe)
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT).build());
    }
}
