package br.com.hahn.toxicbet.domain.repository;

import br.com.hahn.toxicbet.domain.model.Users;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface UserRepository extends ReactiveCrudRepository<Users, UUID> {

    Mono<Users> findByEmail(String email);

    Mono<Boolean> existsByEmail(String email);

    @Modifying
    @Query("UPDATE users SET user_points = COALESCE(user_points, 0) + :points WHERE id = :userId")
    Mono<Integer> incrementUserPoints(@Param("userId") UUID userId, @Param("points") Double points);

}
