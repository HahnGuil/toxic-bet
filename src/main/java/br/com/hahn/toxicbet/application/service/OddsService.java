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

    public Mono<Void> updateOddsForBet(Long matchId, Result betResult){
        return matchService.findById(matchId)
                .flatMap(match -> userService.countAllUsers()
                        .flatMap(totalUsers -> {
                            match.setTotalBetMatch(match.getTotalBetMatch() + plusUserValue());
                            return updateOddsForMatch(match, betResult, totalUsers);
                        }))
                .flatMap(matchRepository::save)
                .then();
    }

    private Mono<Match> updateOddsForMatch(Match match, Result betResult, Long totalUser){
        int totalBets = match.getTotalBetMatch();

        return Mono.fromCallable(() -> {
            switch (betResult) {
                case HOME_WIN -> {
                    match.setTotalBetHomeTeam(match.getTotalBetHomeTeam() + 1);
                    int contraryBets = calculateContraryBets(match, betResult);
                    match.setOddsHomeTeam(calculateNewOdd(contraryBets, totalBets, totalUser));
                }
                case VISITING_WIN -> {
                    match.setTotalBetVisitingTeam(match.getTotalBetVisitingTeam() + 1);
                    int contraryBets = calculateContraryBets(match, betResult);
                    match.setOddsVisitingTeam(calculateNewOdd(contraryBets, totalBets, totalUser));
                }
                case DRAW -> {
                    match.setTotalBetDraw(match.getTotalBetMatch() + 1);
                    int contraryBets = calculateContraryBets(match, betResult);
                    match.setOddsDraw(calculateNewOdd(contraryBets, totalBets, totalUser));
                }
                default -> {
                    match.setTotalBetMatch(0);
                    match.setOddsDraw(calculateNewOdd(0, 0, 0L));
                }
            }

            return match;
        });
    }

    private int calculateContraryBets(Match match, Result betResult) {
        int totalBets = match.getTotalBetMatch();

        return switch (betResult) {
            case HOME_WIN -> totalBets - match.getTotalBetHomeTeam();
            case VISITING_WIN -> totalBets - match.getTotalBetVisitingTeam();
            case DRAW -> totalBets - match.getTotalBetDraw();
            default -> totalBets;
        };
    }

    private Double calculateNewOdd(int contraryBets, int totalBets, Long totalUsers) {
        if (totalBets == 0) {
            return BaseValues.ODD_BASE_VALUE.getDoubleValue();
        }
        return ((((double) contraryBets / totalBets) * totalUsers) / 100) * baseOddsValue();
    }

    private Double baseOddsValue(){
        return Objects.requireNonNull(BaseValues.ODD_BASE_VALUE.getDoubleValue());
    }

    private Integer plusUserValue(){
        return  Objects.requireNonNull(BaseValues.PLUS_USER_VALUE.getIntValue());
    }
}
