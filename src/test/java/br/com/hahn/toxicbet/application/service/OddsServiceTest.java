package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.domain.model.Match;
import br.com.hahn.toxicbet.domain.model.enums.Result;
import br.com.hahn.toxicbet.domain.repository.MatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OddsServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private OddsService service;

    @Test
    void shouldUpdateOddsAndReturnUserPointsForHomeWin() {
        Match match = createBaseMatch();

        when(matchRepository.findById(1L)).thenReturn(Mono.just(match));
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.updateOddsForBet(1L, Result.HOME_WIN, 2.5))
                .expectNext(2.5)
                .verifyComplete();

        ArgumentCaptor<Match> captor = ArgumentCaptor.forClass(Match.class);
        verify(matchRepository).save(captor.capture());

        Match saved = captor.getValue();
        assertEquals(1, saved.getTotalBetMatch());
        assertEquals(1, saved.getTotalBetHomeTeam());
        assertEquals(9.0, saved.getOddsHomeTeam());
        assertEquals(11.0, saved.getOddsDraw());
        assertEquals(11.0, saved.getOddsVisitingTeam());
    }

    @Test
    void shouldNotDecreaseOddsBelowOne() {
        Match match = createBaseMatch();
        match.setOddsDraw(1.0);

        when(matchRepository.findById(1L)).thenReturn(Mono.just(match));
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.updateOddsForBet(1L, Result.DRAW, 1.0))
                .expectNext(1.0)
                .verifyComplete();

        ArgumentCaptor<Match> captor = ArgumentCaptor.forClass(Match.class);
        verify(matchRepository).save(captor.capture());
        assertEquals(1.0, captor.getValue().getOddsDraw());
    }

    private Match createBaseMatch() {
        Match match = new Match();
        match.setId(1L);
        match.setTotalBetMatch(0);
        match.setTotalBetHomeTeam(0);
        match.setTotalBetDraw(0);
        match.setTotalBetVisitingTeam(0);
        match.setOddsHomeTeam(10.0);
        match.setOddsDraw(10.0);
        match.setOddsVisitingTeam(10.0);
        return match;
    }
}

