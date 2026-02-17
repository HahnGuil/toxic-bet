package br.com.hahn.toxicbet.infrastructure.repository;

import br.com.hahn.toxicbet.domain.exception.BusinessException;
import br.com.hahn.toxicbet.domain.model.Match;
import br.com.hahn.toxicbet.domain.model.enums.ErrorMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;


/**
 * Repository responsible for pessimistic locking operations on Match entities.
 * This class implements the Single Responsibility Principle by handling only
 * database locking concerns, separated from business logic.
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class MatchLockRepository {

    private static final String SELECT_MATCH_WITH_LOCK_QUERY = """
            SELECT id, home_team_id, visiting_team_id, home_team_score, visiting_team_score,
                   odds_home_team, odds_visiting_team, odds_draw, championship_id, result,
                   match_time, total_bet_home_team, total_bet_draw, total_bet_visiting_team,
                   total_bet_match, version
            FROM match
            WHERE id = :matchId
            FOR UPDATE
            """;

    private final DatabaseClient databaseClient;
    private final MatchRowMapper matchRowMapper;

    /**
     * Finds a match by ID with pessimistic locking (FOR UPDATE).
     * This ensures no other transaction can modify the match until the current transaction completes.
     *
     * @param matchId The ID of the match to find
     * @return A Mono containing the locked Match entity
     * @throws BusinessException if the match is not found
     */
    public Mono<Match> findByIdWithLock(Long matchId) {
        log.debug("MatchLockRepository: Acquiring lock for match with id: {}", matchId);

        return databaseClient.sql(SELECT_MATCH_WITH_LOCK_QUERY)
                .bind("matchId", matchId)
                .map((row, metadata) -> matchRowMapper.mapRow(row))
                .one()
                .doOnSuccess(match -> log.debug("MatchLockRepository: Lock acquired for match: {}", matchId))
                .switchIfEmpty(Mono.error(new BusinessException(ErrorMessages.MATCH_NOT_FOUND.getMessage())));
    }
}

