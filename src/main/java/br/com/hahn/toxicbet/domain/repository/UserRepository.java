package br.com.hahn.toxicbet.domain.repository;

import br.com.hahn.toxicbet.domain.model.Users;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface UserRepository extends ReactiveCrudRepository<Users, UUID> {

    Mono<Users> findByEmail(String email);

}
