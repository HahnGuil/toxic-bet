package br.com.hahn.toxicbet.domain.repository;

import br.com.hahn.toxicbet.domain.model.Championship;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface ChampionshipRepository extends ReactiveCrudRepository<Championship, Long> {
}
