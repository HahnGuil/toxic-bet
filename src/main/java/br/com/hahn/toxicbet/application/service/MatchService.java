package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.application.mapper.MatchMapper;
import br.com.hahn.toxicbet.domain.exception.*;
import br.com.hahn.toxicbet.domain.model.Championship;
import br.com.hahn.toxicbet.domain.model.Match;
import br.com.hahn.toxicbet.domain.model.Team;
import br.com.hahn.toxicbet.domain.model.enums.BaseValues;
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
    private final ChampionshipService championshipService;


    public Mono<MatchResponseDTO> createMatchDto(Mono<MatchRequestDTO> matchRequestDTOMono) {
        return matchRequestDTOMono
                .flatMap(this::isMatchTimeValid)
                .flatMap(this::validatesTeamNotPlayingAtTheScheduledTime)
                .flatMap(this::validateTeamsAreDifferent)
                .flatMap(this::fetchTeamsAndCreateMatch);
    }

    public Flux<MatchResponseDTO> findAll(){
        return repository.findAll().flatMap(this::buildMatchResponseDTO);
    }

    public Mono<Long> updateMatchesToInProgress(){
        return repository.findAll()
                .filter(match -> match.getResult() == Result.NOT_STARTED)
                .filter(match -> match.getMatchTime().isBefore(LocalDateTime.now()))
                .flatMap(match -> {
                    match.setResult(Result.IN_PROGRESS);
                    match.setOddsHomeTeam(BaseValues.ODD_BASE_VALUE.getDoubleValue());
                    match.setOddsVisitingTeam(BaseValues.ODD_BASE_VALUE.getDoubleValue());
                    match.setOddsDraw(BaseValues.ODD_BASE_VALUE.getDoubleValue());
                    return repository.save(match);
                }).count();
    }

    public Mono<MatchResponseDTO> updateMatchScore(Long matchId, Integer homeScore, Integer visitingScore) {
        return repository.findById(matchId)
                .switchIfEmpty(Mono.error(new NotFoundException(ErrorMessages.MATCH_NOT_FOUND.getMessage() + matchId)))
                .flatMap(match -> validateAndUpdateScore(match, homeScore, visitingScore))
                .flatMap(repository::save)
                .flatMap(this::buildMatchResponseDTO);
    }

    public Flux<MatchResponseDTO> findAllInProgress() {
        return repository.findByResult(Result.IN_PROGRESS)
                .flatMap(this::buildMatchResponseDTO);
    }

    public Mono<Match> findById(Long id){
        return repository.findById(id)
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("MatchService: NOT_FOUND: Not found match for id: {}, throw Not Found Exception at: {}", id, DateTimeConverter.formatInstantNow());
                    return Mono.error(new NotFoundException(ErrorMessages.MATCH_NOT_FOUND.getMessage()));
                }));
    }

    public Mono<MatchResponseDTO> buildMatchResponseDTO(Match match) {
        return Mono.zip(
                teamService.findById(match.getHomeTeamId()),
                teamService.findById(match.getVisitingTeamId()),
                championshipService.findById(match.getChampionshipId())
        ).map(matchResponse -> mapper.toDto(match, matchResponse.getT1().getName(), matchResponse.getT2().getName(), matchResponse.getT3().getName()));
    }

    private Mono<MatchResponseDTO> fetchTeamsAndCreateMatch(MatchRequestDTO dto) {
        return Mono.zip(
                findTeamWithContext(dto.getHomeTeamId(), ErrorMessages.HOME_TEAM_NOT_FOUND),
                findTeamWithContext(dto.getVisitingTeamId(), ErrorMessages.VISITING_TEAM_NOT_FOUND),
                findChampionshipById(dto.getChampionshipId())
        ).flatMap(match -> createAndSaveMatch(dto, match.getT1(), match.getT2(), match.getT3()));
    }

    private Mono<Championship> findChampionshipById(Long championshipId){
        return championshipService.findById(championshipId)
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("MatchService: NOT_FOUND: Championship {}, not found. Throw Not Found Exception at: {} ", championshipId, DateTimeConverter.formatInstantNow());
                    return Mono.error(new NotFoundException(ErrorMessages.CHAMPIONSHIP_NOT_FOUND.getMessage()));
        }));
    }

    private Mono<Team> findTeamWithContext(Long teamId, ErrorMessages errorMessage) {
        return teamService.findById(teamId)
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("MatchService: NOT_FOUND: Team {} not found at: {}", teamId, DateTimeConverter.formatInstantNow());
                    return Mono.error(new NotFoundException(errorMessage.getMessage()));
                }));
    }

    private Mono<MatchResponseDTO> createAndSaveMatch(MatchRequestDTO dto, Team homeTeam, Team visitingTeam, Championship championship) {
        var entity = prepareMatchEntity(dto);
        return repository.save(entity)
                .map(savedMatch -> mapper.toDto(savedMatch, homeTeam.getName(), visitingTeam.getName(), championship.getName()));
    }

    private Match prepareMatchEntity(MatchRequestDTO dto) {
        var entity = mapper.toEntity(dto);
        entity.setResult(Result.NOT_STARTED);
        entity.setHomeTeamScore(BaseValues.INITIAL_ZERO.getIntValue());
        entity.setVisitingTeamScore(BaseValues.INITIAL_ZERO.getIntValue());
        entity.setOddsHomeTeam(BaseValues.ODD_BASE_VALUE.getDoubleValue());
        entity.setOddsDraw(BaseValues.ODD_BASE_VALUE.getDoubleValue());
        entity.setOddsDraw(BaseValues.ODD_BASE_VALUE.getDoubleValue());
        entity.setTotalBetMatch(BaseValues.INITIAL_ZERO.getIntValue());
        return entity;
    }

    private Mono<MatchRequestDTO> isMatchTimeValid(MatchRequestDTO dto) {
        var matchTime = DateTimeConverter.parseToLocalDateTime(dto.getMatchTime());
        assert matchTime != null;
        if (matchTime.isBefore(LocalDateTime.now())) {
            log.error("MatchService: UNPROCESSABLE_ENTITY: Invalid match time, throw InvalidMatchTimeException at: {}", DateTimeConverter.formatInstantNow());
            return Mono.error(new BusinessException(ErrorMessages.INVALID_MATCH_TIME.getMessage()));
        }
        return Mono.just(dto);
    }

    private Mono<MatchRequestDTO> validateTeamsAreDifferent(MatchRequestDTO dto) {
        if (dto.getHomeTeamId().equals(dto.getVisitingTeamId())) {
            log.error("MatchService: UNPROCESSABLE_ENTITY: Team are not different. Throw InvalidTeamException at: {}", DateTimeConverter.formatInstantNow());
            return Mono.error(new BusinessException(ErrorMessages.TEAM_MUST_BE_DIFFERENT.getMessage()));
        }
        return Mono.just(dto);
    }

    private Mono<MatchRequestDTO> validatesTeamNotPlayingAtTheScheduledTime(MatchRequestDTO dto){
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
            log.error("MathService: CONFLICT: Team already involved at match. Throw ConflictMatchTimeException at: {}", DateTimeConverter.formatInstantNow());
            return Mono.error(new ConflictException(ErrorMessages.CONFLICT_MATCH_TIME.getMessage()));
        }
        return Mono.just(dto);
    }

    private Mono<Match> validateAndUpdateScore(Match match, Integer homeScore, Integer visitingScore) {
        if (match.getResult() != Result.IN_PROGRESS) {
            log.error("MatchServe: UNPROCESSABLE_ENTITY: Cannot update match: {}, Throw InvalidMatchStateException at: {}", match.getId(), DateTimeConverter.formatInstantNow());
            return Mono.error(new BusinessException(ErrorMessages.CAN_NOT_UPDATE_MATCH.getMessage()));
        }

        match.setHomeTeamScore(homeScore);
        match.setVisitingTeamScore(visitingScore);

        return Mono.just(match);
    }
}