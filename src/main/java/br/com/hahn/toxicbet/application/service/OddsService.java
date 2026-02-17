package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.domain.exception.BusinessException;
import br.com.hahn.toxicbet.domain.model.Match;
import br.com.hahn.toxicbet.domain.model.enums.BaseValues;
import br.com.hahn.toxicbet.domain.model.enums.ErrorMessages;
import br.com.hahn.toxicbet.domain.model.enums.Result;
import br.com.hahn.toxicbet.domain.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.LocalDateTime;
import java.util.Objects;


@Service
@Slf4j
@RequiredArgsConstructor
public class OddsService {

    private final MatchRepository matchRepository;
    private final DatabaseClient databaseClient;

    public Mono<Double> updateOddsForBet(Long matchId, Result result, Double currentOdds) {
        return findMatchWithLock(matchId)
                .flatMap(match -> calculatedOdds(match, result, currentOdds))
                .flatMap(tuple -> matchRepository.save(tuple.getT1())
                        .doOnSuccess(saved -> log.debug(
                                "OddsService: Match {} updated. Total bets: {} (Home: {}, Draw: {}, Away: {})",
                                saved.getId(),
                                saved.getTotalBetMatch(),
                                saved.getTotalBetHomeTeam(),
                                saved.getTotalBetDraw(),
                                saved.getTotalBetVisitingTeam()
                        ))
                        .thenReturn(tuple.getT2()));
    }

    private Mono<Match> findMatchWithLock(Long matchId) {
        return databaseClient.sql("""
                    SELECT id, home_team_id, visiting_team_id, home_team_score, visiting_team_score,
                           odds_home_team, odds_visiting_team, odds_draw, championship_id, result,
                           match_time, total_bet_home_team, total_bet_draw, total_bet_visiting_team,
                           total_bet_match, version
                    FROM match
                    WHERE id = :matchId
                    FOR UPDATE
                    """)
                .bind("matchId", matchId)
                .map((row, metadata) -> {
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

                    String resultStr = row.get("result", String.class);
                    match.setResult(resultStr != null ? Result.valueOf(resultStr) : null);

                    match.setMatchTime(row.get("match_time", LocalDateTime.class));
                    match.setTotalBetHomeTeam(row.get("total_bet_home_team", Integer.class));
                    match.setTotalBetDraw(row.get("total_bet_draw", Integer.class));
                    match.setTotalBetVisitingTeam(row.get("total_bet_visiting_team", Integer.class));
                    match.setTotalBetMatch(row.get("total_bet_match", Integer.class));
                    match.setVersion(row.get("version", Integer.class));

                    return match;
                })
                .one()
                .switchIfEmpty(Mono.error(new BusinessException(ErrorMessages.MATCH_NOT_FOUND.getMessage())));
    }

    private Mono<Tuple2<Match, Double>> calculatedOdds(Match match, Result betResult, Double oddsUser){
        match.setTotalBetMatch(match.getTotalBetMatch() + 1);
        int totalBets = match.getTotalBetMatch();

        log.debug("OddsService: Processing bet for match {}. Current total: {}", match.getId(), totalBets);

        return Mono.fromCallable(() -> {
            Double userPoints = 0.0;
            switch (betResult) {
                case HOME_WIN -> {
                    log.info("OddserService: VOTANDO NO TIME DA CASA {}", betResult);
                    match.setTotalBetHomeTeam(match.getTotalBetHomeTeam() + plusValue());
                    int contraryBets = calculateContraryBets(match, betResult, totalBets);
                    userPoints = calculateUserPoints(contraryBets, totalBets, oddsUser);
                    match.setOddsHomeTeam(decreaseOdds(match.getOddsHomeTeam()));
                    match.setOddsDraw(increaseOdds(match.getOddsDraw()));
                    match.setOddsVisitingTeam(increaseOdds(match.getOddsVisitingTeam()));
                }
                case DRAW -> {
                    log.info("OddserService: VOTANDO NO EMPATE {}", betResult);
                    match.setTotalBetDraw(match.getTotalBetDraw() + plusValue());
                    int contraryBets = calculateContraryBets(match, betResult, totalBets);
                    userPoints = calculateUserPoints(contraryBets, totalBets, oddsUser);
                    match.setOddsVisitingTeam(increaseOdds(match.getOddsVisitingTeam()));
                    match.setOddsHomeTeam(increaseOdds(match.getOddsHomeTeam()));
                    match.setOddsDraw(decreaseOdds(match.getOddsDraw()));
                }
                case VISITING_WIN -> {
                    log.info("OddserService: VOTANDO NO VISITANTE {}", betResult);
                    match.setTotalBetVisitingTeam(match.getTotalBetVisitingTeam() + plusValue());
                    int contraryBets = calculateContraryBets(match, betResult, totalBets);
                    userPoints = calculateUserPoints(contraryBets, totalBets, oddsUser);

                    match.setOddsVisitingTeam(decreaseOdds(match.getOddsVisitingTeam()));
                    match.setOddsDraw(increaseOdds(match.getOddsDraw()));
                    match.setOddsHomeTeam(increaseOdds(match.getOddsHomeTeam()));
                }
                default -> log.warn("OddsService: Unexpected bet result: {}", betResult);
            }
            return Tuples.of(match, userPoints);
        });
    }

    private double increaseOdds(Double oddsToIncrease){
        return oddsToIncrease + adjustmentValue();
    }

    private double decreaseOdds(Double oddsToDecrease){
        if(oddsToDecrease - adjustmentValue() < 1){
            return minimalOddsValue();
        }
        return oddsToDecrease - adjustmentValue();
    }

    private int calculateContraryBets(Match match, Result betResult, Integer totalBets) {
        int contraryBets = switch (betResult) {
            case HOME_WIN -> totalBets - match.getTotalBetHomeTeam();
            case VISITING_WIN -> totalBets - match.getTotalBetVisitingTeam();
            case DRAW -> totalBets - match.getTotalBetDraw();
            default -> totalBets;
        };

        return Math.max(contraryBets, 1);
    }

    private Double calculateUserPoints(int contraryBets, int totalBets, Double oddsUser){
        if (totalBets == 0){
            totalBets = 1;
        }
        double result = (oddsUser * contraryBets) / totalBets;
        double truncatedResult = Math.floor(result * 10.0) / 10.0;
        return Math.max(truncatedResult, 1.0);
    }

    private Double adjustmentValue(){
        return Objects.requireNonNull(BaseValues.ADJUSTMENT_VALUE.getDoubleValue());
    }

    private Integer plusValue(){
        return  Objects.requireNonNull(BaseValues.PLUS_VALUE.getIntValue());
    }

    private Double minimalOddsValue(){
        return Objects.requireNonNull(BaseValues.MINIMAL_ODD_VALEU.getDoubleValue());
    }
}
