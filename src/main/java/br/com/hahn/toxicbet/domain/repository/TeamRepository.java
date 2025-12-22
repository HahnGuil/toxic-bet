package br.com.hahn.toxicbet.domain.repository;

import br.com.hahn.toxicbet.domain.model.Team;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface TeamRepository extends ReactiveCrudRepository<Team, Long> {
}
