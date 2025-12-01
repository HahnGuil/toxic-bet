package br.com.hahn.toxicbet.domain.repository;

import br.com.hahn.toxicbet.domain.model.Users;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends ReactiveCrudRepository<Users, Long> {
}
