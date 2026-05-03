package br.com.hahn.toxicbet.domain.repository;

import br.com.hahn.toxicbet.domain.model.BettingPool;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface BettingPoolRepository extends ReactiveCrudRepository<BettingPool, Long> {

    Mono<BettingPool> findBettingPoolByBettingPoolKey (String bettingPoolKey);

    @Query("SELECT * FROM betting_pool bp WHERE :userId = ANY(bp.user_ids)")
    Flux<BettingPool> findAllByUserId(String userId);

    default Mono<Integer> totalBettingPool(){
        return count().map(Math::toIntExact);
    }

}
