package br.com.hahn.toxicbet.domain.repository;

import br.com.hahn.toxicbet.domain.model.BettingPool;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface BettingPoolRepository extends ReactiveCrudRepository<BettingPool, Long> {

    Mono<BettingPool> findBettingPoolByBettingPoolKey (String bettingPoolKey);

}
