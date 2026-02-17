package br.com.hahn.toxicbet.infrastructure.repository;

import br.com.hahn.toxicbet.domain.model.Match;
import br.com.hahn.toxicbet.domain.model.enums.Result;
import io.r2dbc.spi.Row;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mapper responsible for converting database rows to Match entities.
 * This class implements the Single Responsibility Principle by handling only
 * the mapping logic between database rows and domain objects.
 */
@Component
public class MatchRowMapper {

    /**
     * Maps a database row to a Match entity.
     *
     * @param row The database row
     * @return A fully populated Match entity
     */
    public Match mapRow(Row row) {
        Match match = new Match();

        match.setId(row.get("id", Long.class));
        match.setHomeTeamId(row.get("home_team_id", Long.class));
        match.setVisitingTeamId(row.get("visiting_team_id", Long.class));
        match.setHomeTeamScore(row.get("home_team_score", Integer.class));
        match.setVisitingTeamScore(row.get("visiting_team_score", Integer.class));
        match.setOddsHomeTeam(row.get("odds_home_team", Double.class));
        match.setOddsVisitingTeam(row.get("odds_visiting_team", Double.class));
        match.setOddsDraw(row.get("odds_draw", Double.class));
        match.setChampionshipId(row.get("championship_id", Long.class));
        match.setMatchTime(row.get("match_time", LocalDateTime.class));
        match.setTotalBetHomeTeam(row.get("total_bet_home_team", Integer.class));
        match.setTotalBetDraw(row.get("total_bet_draw", Integer.class));
        match.setTotalBetVisitingTeam(row.get("total_bet_visiting_team", Integer.class));
        match.setTotalBetMatch(row.get("total_bet_match", Integer.class));
        match.setVersion(row.get("version", Integer.class));

        String resultStr = row.get("result", String.class);
        match.setResult(resultStr != null ? Result.valueOf(resultStr) : null);

        return match;
    }
}

