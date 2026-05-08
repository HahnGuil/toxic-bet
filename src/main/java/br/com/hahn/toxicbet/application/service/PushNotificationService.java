package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.domain.exception.NotFoundException;
import br.com.hahn.toxicbet.domain.model.PushSubscription;
import br.com.hahn.toxicbet.domain.model.dto.OpenBetNotificationTarget;
import br.com.hahn.toxicbet.domain.model.enums.ErrorMessages;
import br.com.hahn.toxicbet.domain.model.enums.NotificationMessages;
import br.com.hahn.toxicbet.domain.repository.PushSubscriptionRepository;
import br.com.hahn.toxicbet.domain.repository.UserRepository;
import br.com.hahn.toxicbet.infrastructure.config.PushNotificationProperties;
import br.com.hahn.toxicbet.util.DateTimeConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.apache.http.HttpResponse;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PushNotificationService {

    private static final String OPEN_BETS_TITLE = "Partidas abertas";
    private static final int HTTP_GONE = 410;
    private static final int HTTP_NOT_FOUND = 404;

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final UserRepository userRepository;
    private final DatabaseClient databaseClient;
    private final PushNotificationProperties properties;
    private final ObjectMapper objectMapper;
    private volatile PushService pushService;

    public Mono<String> getPublicKey() {
        if (!isEnabled()) {
            return Mono.empty();
        }
        return Mono.just(properties.publicKey());
    }

    public Mono<Void> subscribe(PushSubscriptionRequest request, String userEmail, String userAgent) {
        if (!isEnabled()) {
            log.info("PushNotificationService: Push subscription ignored because notifications are disabled");
            return Mono.empty();
        }

        return userRepository.findByEmail(userEmail)
                .switchIfEmpty(Mono.error(new NotFoundException(ErrorMessages.USER_NOT_FOUND.getMessage())))
                .flatMap(user -> pushSubscriptionRepository.findByEndpoint(request.endpoint())
                        .defaultIfEmpty(new PushSubscription())
                        .flatMap(subscription -> {
                            LocalDateTime now = DateTimeConverter.nowBrasilia();
                            if (subscription.getCreatedAt() == null) {
                                subscription.setCreatedAt(now);
                            }
                            subscription.setUserId(user.getId());
                            subscription.setEndpoint(request.endpoint());
                            subscription.setP256dh(request.keys().p256dh());
                            subscription.setAuth(request.keys().auth());
                            subscription.setUserAgent(userAgent);
                            subscription.setActive(true);
                            subscription.setUpdatedAt(now);
                            return pushSubscriptionRepository.save(subscription);
                        }))
                .then();
    }

    public Mono<Void> unsubscribe(PushSubscriptionDeleteRequest request) {
        return pushSubscriptionRepository.deleteByEndpoint(request.endpoint()).then();
    }

    public Mono<Long> notifyOpenBets() {
        if (!isEnabled()) {
            log.info("PushNotificationService: Skipping open bets notification because push is disabled");
            return Mono.just(0L);
        }

        return findOpenBetNotificationTargets()
                .flatMap(this::sendOpenBetNotification, properties.effectiveSendConcurrency())
                .filter(Boolean::booleanValue)
                .count()
                .doOnSuccess(count -> log.info("PushNotificationService: Sent {} open bets notifications", count));
    }

    private boolean isEnabled() {
        return properties.enabled() && properties.hasVapidKeys();
    }

    private Flux<OpenBetNotificationTarget> findOpenBetNotificationTargets() {
        LocalDateTime now = DateTimeConverter.nowBrasilia();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime startOfNextDay = startOfDay.plusDays(1);

        return databaseClient.sql("""
                        SELECT
                            ps.id AS subscription_id,
                            ps.endpoint,
                            ps.p256dh,
                            ps.auth,
                            c.name AS championship_name,
                            COUNT(m.id) AS open_matches
                        FROM push_subscription ps
                        JOIN match m
                          ON m.result = 'OPEN_FOR_BETTING'
                         AND m.match_time > :now
                         AND m.match_time >= :startOfDay
                         AND m.match_time < :startOfNextDay
                        JOIN championship c
                          ON c.id = m.championship_id
                        WHERE ps.active = TRUE
                          AND NOT EXISTS (
                              SELECT 1
                              FROM bet b
                              WHERE b.user_id = ps.user_id
                                AND b.match_id = m.id
                          )
                        GROUP BY ps.id, ps.endpoint, ps.p256dh, ps.auth, c.name
                        HAVING COUNT(m.id) > 0
                        """)
                .bind("now", now)
                .bind("startOfDay", startOfDay)
                .bind("startOfNextDay", startOfNextDay)
                .map((row, metadata) -> new OpenBetNotificationTarget(
                        row.get("subscription_id", Long.class),
                        row.get("endpoint", String.class),
                        row.get("p256dh", String.class),
                        row.get("auth", String.class),
                        row.get("championship_name", String.class),
                        readCount(row.get("open_matches"))
                ))
                .all();
    }

    private long readCount(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private Mono<Boolean> sendOpenBetNotification(OpenBetNotificationTarget target) {
        return Mono.fromCallable(() -> send(target))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(status -> handlePushStatus(target.subscriptionId(), status))
                .onErrorResume(error -> {
                    log.error("PushNotificationService: Failed to send notification to subscription {}: {}",
                            target.subscriptionId(), error.getMessage());
                    return pushSubscriptionRepository.markFailure(target.subscriptionId(), DateTimeConverter.nowBrasilia())
                            .thenReturn(false);
                });
    }

    private int send(OpenBetNotificationTarget target) throws Exception {
        Subscription subscription = new Subscription(
                target.endpoint(),
                new Subscription.Keys(target.p256dh(), target.auth())
        );
        Notification notification = Notification.builder()
                .endpoint(subscription.endpoint)
                .userPublicKey(subscription.keys.p256dh)
                .userAuth(subscription.keys.auth)
                .payload(buildPayload(target))
                .ttl(properties.effectiveTtlSeconds())
                .build();
        HttpResponse response = getPushService().send(notification);
        return response.getStatusLine().getStatusCode();
    }

    private PushService getPushService() throws Exception {
        PushService current = pushService;
        if (current != null) {
            return current;
        }

        synchronized (this) {
            if (pushService == null) {
                pushService = new PushService(properties.publicKey(), properties.privateKey(), properties.vapidSubject());
            }
            return pushService;
        }
    }

    private String buildPayload(OpenBetNotificationTarget target) throws JsonProcessingException {
        String body = NotificationMessages.openBets(target.openMatches(), target.championshipName());
        return objectMapper.writeValueAsString(Map.of(
                "notification", Map.of(
                        "title", OPEN_BETS_TITLE,
                        "body", body,
                        "icon", "/icons/icon-192x192.png",
                        "badge", "/icons/icon-72x72.png",
                        "data", Map.of(
                                "url", "/match",
                                "onActionClick", Map.of(
                                        "default", Map.of(
                                                "operation", "openWindow",
                                                "url", "/match"
                                        )
                                )
                        )
                )
        ));
    }

    private Mono<Boolean> handlePushStatus(Long subscriptionId, int status) {
        LocalDateTime now = DateTimeConverter.nowBrasilia();
        if (status == HTTP_GONE || status == HTTP_NOT_FOUND) {
            return pushSubscriptionRepository.deactivate(subscriptionId, now).thenReturn(false);
        }
        if (status >= 200 && status < 300) {
            return pushSubscriptionRepository.markSuccess(subscriptionId, now).thenReturn(true);
        }
        return pushSubscriptionRepository.markFailure(subscriptionId, now).thenReturn(false);
    }

    public record PushSubscriptionRequest(String endpoint, PushSubscriptionKeys keys) {
    }

    public record PushSubscriptionKeys(String p256dh, String auth) {
    }

    public record PushSubscriptionDeleteRequest(String endpoint) {
    }
}
