package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.domain.model.Match;
import br.com.hahn.toxicbet.domain.model.enums.BaseValues;
import br.com.hahn.toxicbet.domain.model.enums.Result;
import br.com.hahn.toxicbet.domain.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;


@Service
@Slf4j
@RequiredArgsConstructor
public class OddsService {

    private final MatchRepository matchRepository;
    private final UserService userService;
    private final MatchService matchService;

    public Mono<Void> updateOddsForBet(Long matchId, Result betResult, Double odds){
        return matchService.findById(matchId)
                .flatMap(match -> userService.countAllUsers()
                        .flatMap(totalUsers -> {
                            match.setTotalBetMatch(match.getTotalBetMatch() + plusUserValue());
                            return calculatedOdds(match, betResult, odds);
                        }))
                .flatMap(matchRepository::save)
                .then();
    }

    private Mono<Match> calculatedOdds(Match match, Result betResult, Double oddsUser){
        int totalBets = match.getTotalBetMatch() + 1;

        log.debug("OddsService: Total bets for match: {} is: {}", match.getId(), totalBets);

        return Mono.fromCallable(() -> {
            switch (betResult) {
                case HOME_WIN -> {
                    match.setTotalBetHomeTeam(match.getTotalBetHomeTeam() + 1);
                    int contraryBets = calculateContraryBets(match, betResult, totalBets);
                    Double userPoints = calculateUserPoints(contraryBets, totalBets, oddsUser);
                    match.setOddsHomeTeam(match.getOddsHomeTeam() - 1.0);

                }
                case DRAW -> {
                    match.setTotalBetDraw(match.getTotalBetDraw() + 1);
                    int contraryBets = calculateContraryBets(match, betResult, totalBets);
                    Double userPoints = calculateUserPoints(contraryBets, totalBets, oddsUser);
                    match.setOddsVisitingTeam(match.getOddsVisitingTeam() - 1.0);
                }
                case VISITING_WIN -> {
                    match.setTotalBetVisitingTeam(match.getTotalBetVisitingTeam() + 1);
                    int contraryBets = calculateContraryBets(match, betResult, totalBets);
                    Double userPoints = calculateUserPoints(contraryBets, totalBets, oddsUser);
                }
                default -> {
                    match.setTotalBetMatch(0);
                    match.setOddsDraw(calculateNewOdd(0, 0, 0L, 0.0));
                    Double userPoints = 0.0;
                }
            }

            return  match;
        });
    }

//    private double updateOdds(Match match, Result betResult){
//        return newOdd -> {
//            switch (betResult) {
//                case HOME_WIN ->
//            }
//        }
//    }

    private int calculateContraryBets(Match match, Result betResult, Integer totalBets) {

        return switch (betResult) {
            case HOME_WIN -> totalBets - match.getTotalBetHomeTeam();
            case VISITING_WIN -> totalBets - match.getTotalBetVisitingTeam();
            case DRAW -> totalBets - match.getTotalBetDraw();
            default -> totalBets;
        };
    }

    private Double calculateUserPoints(int contraryBets, int totalBets, Double oddsUser){
        if (totalBets == 0){
            totalBets = 1;
        }
        return (oddsUser * contraryBets) / totalBets;
    }

    private Double calculateNewOdd(int contraryBets, int totalBets, Long totalUsers, Double oddsUser) {
        if (totalBets == 0) {
            return BaseValues.ODD_BASE_VALUE.getDoubleValue();
        }
        return (((((double) contraryBets / totalBets) * totalUsers) / 100) * baseOddsValue()) * oddsUser;
    }

    private Double baseOddsValue(){
        return Objects.requireNonNull(BaseValues.ODD_BASE_VALUE.getDoubleValue());
    }

    private Integer plusUserValue(){
        return  Objects.requireNonNull(BaseValues.PLUS_USER_VALUE.getIntValue());
    }
}
