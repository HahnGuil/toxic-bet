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

    private final TopicServiceSend topicServiceSend;

    private static final String OAUTH_USER_TYPE = "OAUTH_USER";
    private static final String TOXIC_BET_APP = "toxic-bet";
    private static final String APPLICATION_CODE = "1";

    public Mono<Void> updateOAuthUserApplication() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(auth -> (Jwt) auth.getPrincipal())
                .flatMap(jwt -> {
                    String typeUser = jwt.getClaim("type_user");
                    boolean hasApp = hasApplication(jwt);

                    log.info("JwtService: Processing user - type: {}, hasApp: {}", typeUser, hasApp);

                    if (OAUTH_USER_TYPE.equals(typeUser) && !hasApp) {
                        log.info("JwtService: User is OAuth and does not have toxic-bet app.  Sending Kafka message.");
                        return Mono.fromRunnable(() ->
                                topicServiceSend.updateOAuthUserApplicatioToToxicBet(
                                        new UserSyncEvent(jwt.getClaim("user_id"), APPLICATION_CODE)
                                )
                        );
                    }

                    log.info("JwtService: User already has application or is not OAuth. Skipping.");
                    return Mono.empty();
                })
                .doOnError(e -> log.error("JwtService: Error processing JWT:  {}", e.getMessage(), e))
                .onErrorResume(e -> {
                    log.warn("JwtService: Continuing despite error in OAuth check");
                    return Mono.empty();
                })
                .then();
    }

    private boolean hasApplication(Jwt jwt) {
        List<String> applications = jwt.getClaim("applications");
        log.debug("JwtService: User applications:  {}", applications);

        if (applications == null || applications.isEmpty()) {
            return false;
        }
        return applications.contains(TOXIC_BET_APP);
    }
}
