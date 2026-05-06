package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.application.mapper.MatchMapper;
import br.com.hahn.toxicbet.domain.model.Championship;
import br.com.hahn.toxicbet.domain.model.Match;
import br.com.hahn.toxicbet.domain.model.Team;
import br.com.hahn.toxicbet.domain.model.enums.Result;
import br.com.hahn.toxicbet.domain.repository.MatchRepository;
import br.com.hahn.toxicbet.model.MatchResponseDTO;
import br.com.hahn.toxicbet.util.DateTimeConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private TeamService teamService;

    @Mock
    private MatchRepository repository;

    @Mock
    private MatchMapper mapper;

    @Mock
    private ChampionshipService championshipService;

    @Mock
    private UserService userService;

    @Mock
    private MatchEventPublisherService matchEventPublisherService;

    @InjectMocks
    private MatchService service;

    @Test
    void shouldOpenOnlyTodayNotStartedFutureMatchesForBetting() {
        LocalDateTime now = DateTimeConverter.nowBrasilia();
        Match todayFuture = match(1L, now.toLocalDate().atTime(23, 59, 59), Result.NOT_STARTED);
        Match todayStarted = match(2L, now.minusMinutes(10), Result.NOT_STARTED);
        Match tomorrow = match(3L, now.toLocalDate().plusDays(1).atTime(10, 0), Result.NOT_STARTED);
        Match alreadyOpen = match(4L, now.toLocalDate().atTime(23, 58), Result.OPEN_FOR_BETTING);

        when(repository.findAll()).thenReturn(Flux.just(todayFuture, todayStarted, tomorrow, alreadyOpen));
        when(repository.findById(1L)).thenReturn(Mono.just(todayFuture));
        when(repository.save(any(Match.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(teamService.findById(anyLong())).thenReturn(Mono.just(namedTeam()));
        when(championshipService.findById(anyLong())).thenReturn(Mono.just(namedChampionship()));
        when(mapper.toDto(any(Match.class), anyString(), anyString(), anyString())).thenReturn(new MatchResponseDTO());

        StepVerifier.create(service.autoOpenMatchToBets())
                .expectNext(1L)
                .verifyComplete();

        ArgumentCaptor<Match> matchCaptor = ArgumentCaptor.forClass(Match.class);
        verify(repository).save(matchCaptor.capture());
        assertEquals(1L, matchCaptor.getValue().getId());
        assertEquals(Result.OPEN_FOR_BETTING, matchCaptor.getValue().getResult());
        verify(repository, never()).findById(2L);
        verify(repository, never()).findById(3L);
        verify(repository, never()).findById(4L);
        verify(matchEventPublisherService).publishMatchUpdate(any(MatchResponseDTO.class));
    }

    private Match match(Long id, LocalDateTime matchTime, Result result) {
        Match match = new Match();
        match.setId(id);
        match.setHomeTeamId(10L);
        match.setVisitingTeamId(20L);
        match.setChampionshipId(30L);
        match.setMatchTime(matchTime);
        match.setResult(result);
        return match;
    }

    private Team namedTeam() {
        Team team = new Team();
        team.setName("Team");
        return team;
    }

    private Championship namedChampionship() {
        Championship championship = new Championship();
        championship.setName("Championship");
        return championship;
    }
}
