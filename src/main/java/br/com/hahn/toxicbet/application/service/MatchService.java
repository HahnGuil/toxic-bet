package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.application.mapper.MatchMapper;
import br.com.hahn.toxicbet.domain.exception.ConflitMathcTimeException;
import br.com.hahn.toxicbet.domain.exception.InvalidMatchTimeException;
import br.com.hahn.toxicbet.domain.exception.InvalidTeamException;
import br.com.hahn.toxicbet.domain.exception.TeamNotFoundException;
import br.com.hahn.toxicbet.domain.model.Match;
import br.com.hahn.toxicbet.domain.model.Team;
import br.com.hahn.toxicbet.domain.model.enums.ErrorMessages;
import br.com.hahn.toxicbet.domain.model.enums.Result;
import br.com.hahn.toxicbet.domain.repository.MatchRepository;
import br.com.hahn.toxicbet.model.MatchRequestDTO;
import br.com.hahn.toxicbet.model.MatchResponseDTO;
import br.com.hahn.toxicbet.util.DateTimeConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchService {

    private final TeamService teamService;
    private final MatchRepository repository;
    private final MatchMapper mapper;

    private static final Integer INITIAL_SCORE = 0;

    public Mono<MatchResponseDTO> createMatchDto(Mono<MatchRequestDTO> matchRequestDTOMono) {
        return matchRequestDTOMono
                .flatMap(this::isMatchTimeValid)
                .flatMap(this::validatesTeamNotPlayingAtTheScheduledTime)
                .flatMap(this::validateTeamsAreDifferent)
                .flatMap(this::fetchTeamsAndCreateMatch);
    }

    public Flux<MatchResponseDTO> findAll(){
        log.info("MatchService: Find all matches at: {}", DateTimeConverter.formatInstantNow());
        return repository.findAll()
                .flatMap(match -> {
                    Mono<Team> home = teamService.findById(match.getHomeTeamId());
                    Mono<Team> visiting = teamService.findById(match.getVisitingTeamId());

                    return Mono.zip(home, visiting)
                            .map(tuple -> mapper.toDto(match, tuple.getT1().getName(), tuple.getT2().getName()));
                });
    }

    public Mono<Long> updateMatchesToInProgress(){
        log.info("MatchService: Updating matches to IN_PROGRESS, at: {}", DateTimeConverter.formatInstantNow());

        return repository.findAll()
                .filter(match -> match.getResult() == Result.NOT_STARTED)
                .filter(match -> match.getMatchTime().isBefore(LocalDateTime.now()))
                .flatMap(match -> {
                    match.setResult(Result.IN_PROGRESS);
                    return repository.save(match);
                }).count();
    }

    private Mono<MatchResponseDTO> fetchTeamsAndCreateMatch(MatchRequestDTO dto) {
        return Mono.zip(findHomeTeam(dto.getHomeTeamId()), findVisitingTeam(dto.getVisitingTeamId()))
                .flatMap(teams -> createAndSaveMatch(dto, teams.getT1(), teams.getT2()));
    }

    private Mono<Team> findHomeTeam(Long homeTeamId) {
        return teamService.findById(homeTeamId)
                .switchIfEmpty(Mono.error(new TeamNotFoundException(ErrorMessages.HOME_TEAM_NOT_FOUND.getMessage())));
    }

    private Mono<Team> findVisitingTeam(Long visitingTeamId) {
        return teamService.findById(visitingTeamId)
                .switchIfEmpty(Mono.error(new TeamNotFoundException(ErrorMessages.VISITING_TEAM_NOT_FOUND.getMessage())));
    }

    private Mono<MatchResponseDTO> createAndSaveMatch(MatchRequestDTO dto, Team homeTeam, Team visitingTeam) {
        var entity = prepareMatchEntity(dto);
        return repository.save(entity)
                .map(savedMatch -> mapper.toDto(savedMatch, homeTeam.getName(), visitingTeam.getName()));
    }

    private Match prepareMatchEntity(MatchRequestDTO dto) {
        var entity = mapper.toEntity(dto);
        entity.setResult(Result.NOT_STARTED);
        entity.setHomeTeamScore(INITIAL_SCORE);
        entity.setVisitingTeamScore(INITIAL_SCORE);
        return entity;
    }

    private Mono<MatchRequestDTO> isMatchTimeValid(MatchRequestDTO dto) {
        log.info("MatchService: Validating match time at: {}", DateTimeConverter.formatInstantNow());
        var matchTime = DateTimeConverter.parseToLocalDateTime(dto.getMatchTime());
        assert matchTime != null;
        if (matchTime.isBefore(LocalDateTime.now())) {
            return Mono.error(new InvalidMatchTimeException("Match time has already passed"));
        }
        return Mono.just(dto);
    }

    private Mono<MatchRequestDTO> validateTeamsAreDifferent(MatchRequestDTO dto) {
        log.info("MatchService: Validating teams are different at: {}", DateTimeConverter.formatInstantNow());
        if (dto.getHomeTeamId().equals(dto.getVisitingTeamId())) {
            return Mono.error(new InvalidTeamException(ErrorMessages.TEAM_MUST_BE_DIFFERENT.getMessage()));
        }
        return Mono.just(dto);
    }

    private Mono<MatchRequestDTO> validatesTeamNotPlayingAtTheScheduledTime(MatchRequestDTO dto){
        log.info("MatchService: Validating team not playing at the scheduled time: {} at: {}", dto.getMatchTime(), DateTimeConverter.formatInstantNow());
        var matchTime = DateTimeConverter.parseToLocalDateTime(dto.getMatchTime());
        assert matchTime != null;
        return hasTeamConflictInTimeWindow(dto, matchTime)
                .flatMap(hasConflict -> handleConflictResult(hasConflict, dto));
    }

    private Mono<Boolean> hasTeamConflictInTimeWindow(MatchRequestDTO dto, LocalDateTime matchTime) {
        var startWindow = matchTime.minusHours(3);
        var endWindow = matchTime.plusHours(3);

        return repository.findAll()
                .filter(match -> isTeamInvolved(match, dto))
                .filter(match -> isInTimeWindow(match.getMatchTime(), startWindow, endWindow))
                .hasElements();
    }

    private boolean isTeamInvolved(Match match, MatchRequestDTO dto) {
        return isHomeTeamInvolved(match, dto) || isVisitingTeamInvolved(match, dto);
    }

    private boolean isHomeTeamInvolved(Match match, MatchRequestDTO dto) {
        return match.getHomeTeamId().equals(dto.getHomeTeamId())
                || match.getVisitingTeamId().equals(dto.getHomeTeamId());
    }

    private boolean isVisitingTeamInvolved(Match match, MatchRequestDTO dto) {
        return match.getHomeTeamId().equals(dto.getVisitingTeamId())
                || match.getVisitingTeamId().equals(dto.getVisitingTeamId());
    }

    private boolean isInTimeWindow(LocalDateTime matchTime, LocalDateTime startWindow, LocalDateTime endWindow) {
        return (matchTime.isAfter(startWindow) || matchTime.isEqual(startWindow))
                && (matchTime.isBefore(endWindow) || matchTime.isEqual(endWindow));
    }

    private Mono<MatchRequestDTO> handleConflictResult(boolean hasConflict, MatchRequestDTO dto) {
        if (hasConflict) {
            return Mono.error(new ConflitMathcTimeException(ErrorMessages.CONFLICT_MATCH_TIME.getMessage()));
        }
        return Mono.just(dto);
    }
}