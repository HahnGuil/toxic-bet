package br.com.hahn.toxicbet.domain.repository;

import br.com.hahn.toxicbet.domain.model.PushSubscription;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface PushSubscriptionRepository extends ReactiveCrudRepository<PushSubscription, Long> {

    Mono<PushSubscription> findByEndpoint(String endpoint);

    @Modifying
    @Query("DELETE FROM push_subscription WHERE endpoint = :endpoint")
    Mono<Integer> deleteByEndpoint(@Param("endpoint") String endpoint);

    @Modifying
    @Query("""
            UPDATE push_subscription
            SET active = FALSE, updated_at = :now, last_failure_at = :now
            WHERE id = :subscriptionId
            """)
    Mono<Integer> deactivate(@Param("subscriptionId") Long subscriptionId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("""
            UPDATE push_subscription
            SET active = TRUE, updated_at = :now, last_success_at = :now
            WHERE id = :subscriptionId
            """)
    Mono<Integer> markSuccess(@Param("subscriptionId") Long subscriptionId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("""
            UPDATE push_subscription
            SET updated_at = :now, last_failure_at = :now
            WHERE id = :subscriptionId
            """)
    Mono<Integer> markFailure(@Param("subscriptionId") Long subscriptionId, @Param("now") LocalDateTime now);
}
