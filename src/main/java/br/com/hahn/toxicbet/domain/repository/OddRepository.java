package br.com.hahn.toxicbet.domain.repository;

import br.com.hahn.toxicbet.domain.model.Odds;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface OddRepository extends ReactiveCrudRepository<Odds, Long> {

    Mono<Odds> findByMatchId(Long matchId);

}
