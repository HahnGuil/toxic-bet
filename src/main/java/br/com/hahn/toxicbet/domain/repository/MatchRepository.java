package br.com.hahn.toxicbet.domain.repository;

import br.com.hahn.toxicbet.domain.model.Match;
import br.com.hahn.toxicbet.domain.model.enums.Result;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface MatchRepository extends ReactiveCrudRepository<Match, Long> {
}
