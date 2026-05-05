package br.com.hahn.toxicbet.domain.repository;

import br.com.hahn.toxicbet.domain.model.Bet;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface BetRepository extends ReactiveCrudRepository<Bet, Long> {

    Flux<Bet> findByMatchId(Long matchId);

    Flux<Bet> findByUserId(UUID userId);

    Mono<Boolean> existsByUserIdAndMatchId(UUID userId, Long matchId);

}
