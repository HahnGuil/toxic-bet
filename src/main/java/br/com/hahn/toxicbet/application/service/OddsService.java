package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.domain.model.Match;
import br.com.hahn.toxicbet.domain.model.enums.BaseValues;
import br.com.hahn.toxicbet.domain.model.enums.MatchType;
import br.com.hahn.toxicbet.domain.model.enums.Result;
import br.com.hahn.toxicbet.domain.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Objects;

/**
 * Service responsible for calculating and updating odds.
 * No longer uses pessimistic locking - sequential processing is guaranteed
 * by BetProcessorService using Sink + concatMap per match.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OddsService {

    private final MatchRepository matchRepository;

    /**     * Updates odds for a bet without locking.     * Sequential processing per match is guaranteed by the caller (BetProcessorService).     * User points will be calculated when match is closed.     *     * @param matchId The match ID     * @param result The bet result     * @return Mono<Void>     */
    public Mono<Void> updateOddsForBet(Long matchId, Result result) {
        return matchRepository.findById(matchId)
                .flatMap(match -> {
                    match.setTotalBetMatch(match.getTotalBetMatch() + 1);

                    log.debug("OddsService: Processing bet for match {}. Current total: {}", match.getId(), match.getTotalBetMatch());

                    switch (result) {
                        case HOME_WIN -> {
                            log.info("OddsService: Bet on HOME_WIN for match {}", match.getId());
                            match.setTotalBetHomeTeam(match.getTotalBetHomeTeam() + plusValue());
                            match.setOddsHomeTeam(decreaseOdds(match.getOddsHomeTeam(), match));
                            match.setOddsDraw(increaseOdds(match.getOddsDraw(), match));
                            match.setOddsVisitingTeam(increaseOdds(match.getOddsVisitingTeam(), match));
                        }
                        case DRAW -> {
                            log.info("OddsService: Bet on DRAW for match {}", match.getId());
                            match.setTotalBetDraw(match.getTotalBetDraw() + plusValue());
                            match.setOddsVisitingTeam(increaseOdds(match.getOddsVisitingTeam(), match));
                            match.setOddsHomeTeam(increaseOdds(match.getOddsHomeTeam(), match));
                            match.setOddsDraw(decreaseOdds(match.getOddsDraw(), match));
                        }
                        case VISITING_WIN -> {
                            log.info("OddsService: Bet on VISITING_WIN for match {}", match.getId());
                            match.setTotalBetVisitingTeam(match.getTotalBetVisitingTeam() + plusValue());
                            match.setOddsVisitingTeam(decreaseOdds(match.getOddsVisitingTeam(), match));
                            match.setOddsDraw(increaseOdds(match.getOddsDraw(), match));
                            match.setOddsHomeTeam(increaseOdds(match.getOddsHomeTeam(), match));
                        }
                        default -> log.warn("OddsService: Unexpected bet result: {}", result);
                    }

                    return matchRepository.save(match)
                            .doOnSuccess(saved -> log.debug(
                                    "OddsService: Match {} updated. Total bets: {} (Home: {}, Draw: {}, Away: {})",
                                    saved.getId(),
                                    saved.getTotalBetMatch(),
                                    saved.getTotalBetHomeTeam(),
                                    saved.getTotalBetDraw(),
                                    saved.getTotalBetVisitingTeam()
                            ))
                            .then();
                });
    }

    private Integer plusValue(){
        return Objects.requireNonNull(BaseValues.PLUS_VALUE.getIntValue());
    }

    private double increaseOdds(Double oddsToIncrease, Match match) {
        if(match.getType().equals(MatchType.PENALTI)){
            return oddsToIncrease + adjustmentPenalValue();
        }
        return oddsToIncrease + adjustmentValue();
    }

    private double decreaseOdds(Double oddsToDecrease, Match match) {
        if(match.getType().equals(MatchType.PENALTI)){
            return decreasePenalOdds(oddsToDecrease);
        }

        if(oddsToDecrease - adjustmentValue() < 1){
            return minimalOddsValue();
        }
        return oddsToDecrease - adjustmentValue();
    }

    private double decreasePenalOdds(Double oddsToDecrease) {
        if(oddsToDecrease - adjustmentValue() < 0.5){
            return minimalPenalValue();
        }
        return oddsToDecrease - adjustmentPenalValue();
    }



    private Double adjustmentValue(){
        return Objects.requireNonNull(BaseValues.ADJUSTMENT_VALUE.getDoubleValue());
    }

    private Double adjustmentPenalValue(){
        return Objects.requireNonNull(BaseValues.PENAL_ODDS_VALUE.getDoubleValue());
    }

    private Double minimalOddsValue(){
        return Objects.requireNonNull(BaseValues.MINIMAL_ODD_VALEU.getDoubleValue());
    }

    private Double minimalPenalValue(){
        return Objects.requireNonNull(BaseValues.MINIMAL_PENAL_ODDS_VALUE.getDoubleValue());
    }
}
