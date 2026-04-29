package br.com.hahn.toxicbet.infrastructure.client;

import br.com.hahn.toxicbet.domain.exception.BusinessException;
import br.com.hahn.toxicbet.infrastructure.config.AuthIntegrationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AuthServiceClient {

    private final AuthIntegrationProperties properties;
    private final WebClient authWebClient;

    public Mono<Void> registerService(String authorizationHeader) {
        String applicationPublicId = properties.getApplicationPublicId();
        return authWebClient.post()
                .uri(uriBuilder -> uriBuilder.path("/auth-server/users/register")
                        .build(applicationPublicId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .header("applicationPublicId", applicationPublicId)
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("No response body")
                        .flatMap(body -> Mono.error(new BusinessException(
                                "Failed to register service in auth provider. status="
                                        + response.statusCode() + ", body=" + body))))
                .bodyToMono(Void.class);
    }
}