package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.domain.model.Match;
import br.com.hahn.toxicbet.domain.model.enums.BaseValues;
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

    public static final double ODD_VALUES = 10;
    private final MatchRepository matchRepository;

    public Mono<Void> updateOddsForBet(Long matchId, Result result) {
        return matchRepository.findById(matchId)
                .flatMap(match -> {
                    match.setTotalBetMatch(match.getTotalBetMatch() + 1);

                    log.debug("OddsService: Processing bet for match {}. Current total: {}", match.getId(), match.getTotalBetMatch());

                    switch (result) {
                        case HOME_WIN -> {
                            log.info("OddsService: Bet on HOME_WIN for match {}", match.getId());
                            match.setTotalBetHomeTeam(match.getTotalBetHomeTeam() + plusValue());
                            match.setOddsHomeTeam(ODD_VALUES);
                            match.setOddsDraw(ODD_VALUES);
                            match.setOddsVisitingTeam(ODD_VALUES);
                        }
                        case DRAW -> {
                            log.info("OddsService: Bet on DRAW for match {}", match.getId());
                            match.setTotalBetDraw(match.getTotalBetDraw() + plusValue());
                            match.setOddsVisitingTeam(ODD_VALUES);
                            match.setOddsHomeTeam(ODD_VALUES);
                            match.setOddsDraw(ODD_VALUES);
                        }
                        case VISITING_WIN -> {
                            log.info("OddsService: Bet on VISITING_WIN for match {}", match.getId());
                            match.setTotalBetVisitingTeam(match.getTotalBetVisitingTeam() + plusValue());
                            match.setOddsVisitingTeam(ODD_VALUES);
                            match.setOddsDraw(ODD_VALUES);
                            match.setOddsHomeTeam(ODD_VALUES);
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
        return  Objects.requireNonNull(BaseValues.PLUS_VALUE.getIntValue());
    }

}
