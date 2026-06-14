package br.com.hahn.toxicbet.domain.repository;

import br.com.hahn.toxicbet.domain.model.Match;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface MatchRepository extends ReactiveCrudRepository<Match, Long> {

    @Query("SELECT * FROM match ORDER BY match_time ASC, id ASC")
    Flux<Match> findAllOrderByMatchTimeAsc();

    @Query("SELECT * FROM match WHERE result = 'OPEN_FOR_BETTING' ORDER BY match_time ASC, id ASC")
    Flux<Match> findOpenForBettingOrderByMatchTimeAsc();

    @Query("SELECT * FROM match WHERE championship_id = :championshipId AND result = 'OPEN_FOR_BETTING' ORDER BY match_time ASC, id ASC")
    Flux<Match> findOpenForBettingByChampionshipOrderByMatchTimeAsc(Long championshipId);
}
