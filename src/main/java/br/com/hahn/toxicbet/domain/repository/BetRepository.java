package br.com.hahn.toxicbet.domain.repository;

import br.com.hahn.toxicbet.domain.model.Bet;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface BetRepository extends ReactiveCrudRepository<Bet, Long> {
}
