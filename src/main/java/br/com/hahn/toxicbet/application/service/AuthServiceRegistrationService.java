package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.infrastructure.client.AuthServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuthServiceRegistrationService {

    private final AuthServiceClient authServiceClient;

    public Mono<Void> registerService(String authorizationHeader) {
        return authServiceClient.registerService(authorizationHeader);
    }
}
