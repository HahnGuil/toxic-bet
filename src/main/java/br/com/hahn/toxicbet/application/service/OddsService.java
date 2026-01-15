package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.domain.model.Odds;
import br.com.hahn.toxicbet.domain.model.enums.Result;
import br.com.hahn.toxicbet.domain.repository.OddRepository;
import br.com.hahn.toxicbet.util.DateTimeConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class OddsService {

    private final OddRepository oddRepository;
    private final UserService userService;
    private static final Double ODD_BASE_VALUE = 5.0;

    public Mono<Odds> createInitialOddsForMatch(Long matchId){
        log.info("OddsService: Creating initial odds for match: {}, at: {}", matchId, DateTimeConverter.formatInstantNow());

        Odds odds = new Odds();
        odds.setMatchId(matchId);
        odds.setTotalBetsForMatch(0);
        odds.setTotalBetsHomeWin(0);
        odds.setTotalBetsVisitingWin(0);
        odds.setTotalBetsDraw(0);
        odds.setOddHomeTeam(ODD_BASE_VALUE);
        odds.setOddDraw(ODD_BASE_VALUE);
        odds.setOddVisitingTeam(ODD_BASE_VALUE);

        return oddRepository.save(odds);
    }


    public Mono<Void> updateOddsForBet(Long matchId, Result betResult){
        log.info("OddsService: Updating odds for match id: {}, at: {}", matchId, DateTimeConverter.formatInstantNow());

        return oddRepository.findByMatchId(matchId)
                .flatMap(odds -> userService.countAllUsers()
                        .flatMap(totalUsers -> {
                            odds.setTotalBetsForMatch(odds.getTotalBetsForMatch() + 1);
                            return updateOddsByResult(odds, betResult, totalUsers);
                        }))
                .flatMap(oddRepository::save)
                .doOnSuccess(odds -> log.info("OddsService: Odds updated successfully for match: {} at: {}", matchId, DateTimeConverter.formatInstantNow()))
                .doOnError(error -> log.error("OddsService: Error updating odds for match: {}, error: {} at: {}", matchId, error.getMessage(), DateTimeConverter.formatInstantNow()))
                .then();
    }

    private Mono<Odds> updateOddsByResult(Odds odds, Result betResult, Long totalUsers) {
        int totalBets = odds.getTotalBetsForMatch();

        return Mono.fromCallable(() -> {
            switch (betResult) {
                case HOME_WIN -> {
                    odds.setTotalBetsHomeWin(odds.getTotalBetsHomeWin() + 1);
                    int contraryBets = calculateContraryBets(odds, betResult);
                    odds.setOddHomeTeam(calculateNewOdd(contraryBets, totalBets, totalUsers));
                }
                case VISITING_WIN -> {
                    odds.setTotalBetsVisitingWin(odds.getTotalBetsVisitingWin() + 1);
                    int contraryBets = calculateContraryBets(odds, betResult);
                    odds.setOddVisitingTeam(calculateNewOdd(contraryBets, totalBets, totalUsers));
                }
                case DRAW -> {
                    odds.setTotalBetsDraw(odds.getTotalBetsDraw() + 1);
                    int contraryBets = calculateContraryBets(odds, betResult);
                    odds.setOddDraw(calculateNewOdd(contraryBets, totalBets, totalUsers));
                }
                default -> {
                    odds.setTotalBetsDraw(0);
                    odds.setOddDraw(calculateNewOdd(0, 0, 0L));
                }
            }
            return odds;
        });
    }


    private int calculateContraryBets(Odds odds, Result betResult) {
        int totalBets = odds.getTotalBetsForMatch();

        // Apostas contrárias = total de apostas - apostas no resultado atual
        return switch (betResult) {
            case HOME_WIN -> totalBets - odds.getTotalBetsHomeWin();
            case VISITING_WIN -> totalBets - odds.getTotalBetsVisitingWin();
            case DRAW -> totalBets - odds.getTotalBetsDraw();
            default -> totalBets;
        };
    }

    private Double calculateNewOdd(int contraryBets, int totalBets, Long totalUsers) {
        if (totalBets == 0) {
            return ODD_BASE_VALUE;
        }
        return ((((double) contraryBets / totalBets) * totalUsers) / 100) * ODD_BASE_VALUE;
    }
}
