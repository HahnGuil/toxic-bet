package br.com.hahn.toxicbet.infrastructure.service;

import br.com.hahn.toxicbet.domain.model.UserSyncEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class JwtService {

//    TODO - CORRIGIR E AJSUTAR OS LOG's

    private final TopicServiceSend topicServiceSend;

    private static final String OAUTH_USER_TYPE = "OAUTH_USER";
    private static final String TOXIC_BET_APP = "toxic-bet";
    private static final String APPLICATION_CODE = "1";

    public Mono<Void> updateOAuthUserApplication() {
        return ReactiveSecurityContextHolder.getContext()
                .doOnNext(ctx -> log.info("SecurityContext obtido: {}", ctx))
                .map(SecurityContext::getAuthentication)
                .doOnNext(auth -> log.info("Authentication obtido: {}, Principal type: {}",
                        auth, auth.getPrincipal().getClass().getName()))
                .map(auth -> (Jwt) auth.getPrincipal())
                .doOnNext(jwt -> log.info("JWT obtido - type_user: {}, applications: {}",
                        jwt.getClaim("type_user"), jwt.getClaim("applications")))
                .flatMap(jwt -> {
                    String typeUser = jwt.getClaim("type_user");
                    boolean hasApp = hasApplication(jwt);

                    log.info("Validação - type_user: {}, hasApplication: {}, esperado: {}",
                            typeUser, hasApp, OAUTH_USER_TYPE);

                    if (OAUTH_USER_TYPE.equals(typeUser) && !hasApp) {
                        log.info("Condições atendidas - enviando para Kafka");
                        return Mono.fromRunnable(() ->
                                topicServiceSend.updateOAuthUserApplicatioToToxicBet(
                                        new UserSyncEvent(jwt.getClaim("user_id"), APPLICATION_CODE)
                                )
                        );
                    }
                    log.info("Condições não atendidas - não enviando para Kafka");
                    return Mono.empty();
                })
                .doOnError(e -> log.error("Erro ao processar JWT", e))
                .onErrorResume(e -> Mono.empty())
                .then();
    }


    private boolean hasApplication(Jwt jwt) {
        List<String> applications = jwt.getClaim("applications");
        log.info("Lista de applications: {}", applications);
        if (applications == null || applications.isEmpty()) {
            return false;
        }
        return applications.contains(TOXIC_BET_APP);
    }
}
