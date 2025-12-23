package br.com.hahn.toxicbet.domain.repository;

import br.com.hahn.toxicbet.domain.model.Match;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface MatchRepository extends ReactiveCrudRepository<Match, Long> {
}
