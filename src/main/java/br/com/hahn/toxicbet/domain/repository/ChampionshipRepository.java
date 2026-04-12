package br.com.hahn.toxicbet.domain.repository;

import br.com.hahn.toxicbet.domain.model.Championship;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ChampionshipRepository extends ReactiveCrudRepository<Championship, Long> {

    Mono<Championship> findByName(String name);

}
