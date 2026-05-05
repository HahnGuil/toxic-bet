package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.application.mapper.MatchMapper;
import br.com.hahn.toxicbet.domain.exception.*;
import br.com.hahn.toxicbet.domain.model.Championship;
import br.com.hahn.toxicbet.domain.model.Match;
import br.com.hahn.toxicbet.domain.model.Team;
import br.com.hahn.toxicbet.domain.model.enums.BaseValues;
import br.com.hahn.toxicbet.domain.model.enums.ErrorMessages;
import br.com.hahn.toxicbet.domain.model.enums.Result;
import br.com.hahn.toxicbet.domain.model.enums.Role;
import br.com.hahn.toxicbet.domain.repository.MatchRepository;
import br.com.hahn.toxicbet.model.CloseMatchRequestDTO;
import br.com.hahn.toxicbet.model.MatchRequestDTO;
import br.com.hahn.toxicbet.model.MatchResponseDTO;
import br.com.hahn.toxicbet.util.DateTimeConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchService {

    private static final Duration BETTING_OPEN_BEFORE_MATCH = Duration.ofHours(2);
    private static final Duration SCHEDULER_LOOKAHEAD = Duration.ofMinutes(30);

    private final TeamService teamService;
    private final MatchRepository repository;
    private final MatchMapper mapper;
    private final ChampionshipService championshipService;
    private final UserService userService;
    private final MatchEventPublisherService matchEventPublisherService;
    private final Map<Long, String> teamNameCache = new ConcurrentHashMap<>();
    private final Map<Long, String> championshipNameCache = new ConcurrentHashMap<>();
    private final Set<Long> pendingOpenTransitions = ConcurrentHashMap.newKeySet();
    private final Set<Long> pendingInProgressTransitions = ConcurrentHashMap.newKeySet();


    public Mono<MatchResponseDTO> createMatchDto(Mono<MatchRequestDTO> matchRequestDTOMono, String userEmail) {
        return matchRequestDTOMono
                .flatMap(dto -> userService.isUserAdmin(userEmail).thenReturn(dto))
                .flatMap(this::isMatchTimeValid)
                .flatMap(this::validatesTeamNotPlayingAtTheScheduledTime)
                .flatMap(this::validateTeamsAreDifferent)
                .flatMap(this::fetchTeamsAndCreateMatch);
    }

    public Flux<MatchResponseDTO> findAll(){
        return repository.findAll().flatMap(this::buildMatchResponseDTO);
    }

    public Flux<MatchResponseDTO> findMatchesOpenToBets(){
        return repository.findAll()
                .filter(match -> match.getResult() == Result.OPEN_FOR_BETTING)
                .flatMap(this::buildMatchResponseDTO);
    }

    public Flux<MatchResponseDTO> findInProgressMatches(){
        return repository.findAll()
                .filter(match -> match.getResult() == Result.IN_PROGRESS)
                .flatMap(this::buildMatchResponseDTO);
    }

    public Mono<Long> autoOpenMatchToBets(){
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime openWindowEnd = now.plus(SCHEDULER_LOOKAHEAD);

        return repository.findAll()
                .filter(match -> match.getResult() == Result.NOT_STARTED)
                .filter(match -> match.getMatchTime().isAfter(now))
                .filter(match -> !getBettingOpenTime(match).isAfter(openWindowEnd))
                .filter(match -> pendingOpenTransitions.add(match.getId()))
                .flatMap(match -> delayUntil(getBettingOpenTime(match))
                        .then(repository.findById(match.getId()))
                        .filter(current -> current.getResult() == Result.NOT_STARTED)
                        .filter(current -> current.getMatchTime().isAfter(LocalDateTime.now()))
                        .flatMap(current -> {
                            current.setResult(Result.OPEN_FOR_BETTING);
                            return repository.save(current)
                                    .flatMap(this::buildMatchResponseDTO)
                                    .doOnNext(matchEventPublisherService::publishMatchUpdate)
                                    .thenReturn(current);
                        })
                        .doFinally(signal -> pendingOpenTransitions.remove(match.getId())))
                .count();
    }

    public Mono<Long> deleteOndMatch() {
        log.info("ApplicationScheduler: Starting to delete old matches at: {}", DateTimeConverter.formatInstantNow());

        LocalDateTime cutoff = LocalDateTime.now().minusDays(2);

        return repository.findAll()
                .filter(match ->
                        match.getResult() == Result.HOME_WIN
                                || match.getResult() == Result.VISITING_WIN
                                || match.getResult() == Result.DRAW
                )
                .filter(match -> match.getMatchTime().isBefore(cutoff))
                .flatMap(repository::delete)
                .count()
                .doOnSuccess(count ->
                        log.info("ApplicationScheduler: Deleted {} old matches at: {}", count, DateTimeConverter.formatInstantNow()));
    }

    public Mono<Long> updateMatchesToInProgress(){
        LocalDateTime closeWindowEnd = LocalDateTime.now().plus(SCHEDULER_LOOKAHEAD);

        return repository.findAll()
                .filter(match -> match.getResult() == Result.OPEN_FOR_BETTING)
                .filter(match -> !match.getMatchTime().isAfter(closeWindowEnd))
                .filter(match -> pendingInProgressTransitions.add(match.getId()))
                .doOnSubscribe(subscription -> log.info("MatchService: Update matches result to In Progress"))
                .flatMap(match -> delayUntil(match.getMatchTime())
                        .then(repository.findById(match.getId()))
                        .filter(current -> current.getResult() == Result.OPEN_FOR_BETTING)
                        .flatMap(current -> {
                            current.setResult(Result.IN_PROGRESS);
                            log.info("MatchService: Update match: {}, with MatchTime: {}, to IN_PROGRESS at: {}", current.getId(), current.getMatchTime(), DateTimeConverter.formatInstantNow());
                            return repository.save(current)
                                    .flatMap(this::buildMatchResponseDTO)
                                    .doOnNext(matchEventPublisherService::publishMatchUpdate)
                                    .thenReturn(current);
                        })
                        .doFinally(signal -> pendingInProgressTransitions.remove(match.getId())))
                .count();
    }

    private LocalDateTime getBettingOpenTime(Match match) {
        return match.getMatchTime().minus(BETTING_OPEN_BEFORE_MATCH);
    }

    private Mono<Void> delayUntil(LocalDateTime transitionTime) {
        Duration delay = Duration.between(LocalDateTime.now(), transitionTime);
        if (!delay.isPositive()) {
            return Mono.empty();
        }
        return Mono.delay(delay).then();
    }

    public Mono<Match> findById(Long id){
        return repository.findById(id)
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("MatchService: NOT_FOUND: Not found match for id: {}, throw Not Found Exception at: {}", id, DateTimeConverter.formatInstantNow());
                    return Mono.error(new NotFoundException(ErrorMessages.MATCH_NOT_FOUND.getMessage()));
                }));
    }

    public Mono<Void> closeBatchMatches(Flux<CloseMatchRequestDTO> requests, String email) {
        return validateBatchCloseAdmin(email)
                .doOnSubscribe(subscription ->
                        log.info("MatchService: User: {}, is closing matches in batch at: {}", email, DateTimeConverter.formatInstantNow()))
                .thenMany(requests.flatMap(req -> {
                    Result finalResult = Result.valueOf(req.getResult().getValue());
                    return repository.findById(req.getMatchId())
                            .switchIfEmpty(Mono.error(new NotFoundException(ErrorMessages.MATCH_NOT_FOUND.getMessage())))
                            .flatMap(match -> {
                                match.setResult(finalResult);
                                return repository.save(match)
                                        .then(userService.calculatedUserPoints(match.getId(), finalResult.name()));
                            });
                }))
                .then()
                .doOnSuccess(success ->
                        log.info("MatchService: Batch close matches completed for user: {} at: {}", email, DateTimeConverter.formatInstantNow()));
    }

    public Mono<Void> closeMatch(Long matchId, String result, String email){
        return userService.isUserAdmin(email)
                .doOnSubscribe(subscription ->
                        log.info("MatchService: User: {}, is close match: {} with result: {} at: {}", email, matchId, result, DateTimeConverter.formatInstantNow()))
                .then(repository.findById(matchId)
                        .switchIfEmpty(Mono.error(new NotFoundException(ErrorMessages.MATCH_NOT_OPEN_TO_BETS.getMessage())))
                        .flatMap(match -> {
                            Result finalResult = Result.valueOf(result);
                            match.setResult(finalResult);

                            return repository.save(match)
                                    .then(userService.calculatedUserPoints(matchId, finalResult.name()));
                        })
                        .then())
                .doOnSuccess(success ->
                        log.info("MatchService: Match: {} is closed with result: {}, for user: {} at: {}", matchId, result, email, DateTimeConverter.formatInstantNow()));
    }


    public Mono<Void> openMatch(Long matchId, String email) {
        return userService.isUserAdmin(email)
                .doOnSubscribe(subscription ->
                        log.info("MatchService: User: {}, is open match: {} for bets at: {}", email, matchId, DateTimeConverter.formatInstantNow()))
                .then(repository.findById(matchId)
                        .switchIfEmpty(Mono.error(new NotFoundException(ErrorMessages.MATCH_NOT_OPEN_TO_BETS.getMessage())))
                        .flatMap(match -> {
                            match.setResult(Result.OPEN_FOR_BETTING);
                            return repository.save(match)
                                    .flatMap(this::buildMatchResponseDTO)
                                    .doOnNext(matchEventPublisherService::publishMatchUpdate)
                                    .then();
                        })
                        .then());
    }

    public Mono<Void> closeMatchForBet(Long matchId, String email) {
        return userService.isUserAdmin(email)
                .doOnSubscribe(subscription ->
                        log.info("MatchService: User: {}, is close match: {} for bets at: {}", email, matchId, DateTimeConverter.formatInstantNow()))
                .then(repository.findById(matchId)
                        .switchIfEmpty(Mono.error(new NotFoundException(ErrorMessages.MATCH_NOT_OPEN_TO_BETS.getMessage())))
                        .flatMap(match -> {
                            match.setResult(Result.IN_PROGRESS);
                            return repository.save(match)
                                    .flatMap(this::buildMatchResponseDTO)
                                    .doOnNext(matchEventPublisherService::publishMatchUpdate)
                                    .then();
                        })
                        .then());
    }

    public Mono<MatchResponseDTO> buildMatchResponseDTO(Match match) {
        return Mono.zip(
                getTeamName(match.getHomeTeamId()),
                getTeamName(match.getVisitingTeamId()),
                getChampionshipName(match.getChampionshipId())
        ).map(matchResponse -> mapper.toDto(match, matchResponse.getT1(), matchResponse.getT2(), matchResponse.getT3()));
    }

    public Flux<MatchResponseDTO> findMatchByChampionship(Long championshipId) {
        return repository.findAll()
                .filter(match -> match.getChampionshipId() != null
                        && match.getChampionshipId().equals(championshipId))
                .flatMap(this::buildMatchResponseDTO);
    }

    public Flux<MatchResponseDTO> findOpenMatchesByChampionship(Long championshipId) {
        return repository.findAll()
                .filter(match -> match.getChampionshipId() != null
                        && match.getChampionshipId().equals(championshipId))
                .filter(match -> Result.OPEN_FOR_BETTING.equals(match.getResult()))
                .flatMap(this::buildMatchResponseDTO);
    }
    public Mono<String> getHomeTeamName(Long homeTeamId) {
        return getTeamName(homeTeamId);
    }

    public Mono<String> getVisitingTeamName(Long visitingTeamId) {
        return getTeamName(visitingTeamId);
    }

    private Mono<String> getTeamName(Long teamId) {
        String cached = teamNameCache.get(teamId);
        if (cached != null) {
            return Mono.just(cached);
        }

        return teamService.findById(teamId)
                .map(Team::getName)
                .doOnNext(name -> teamNameCache.put(teamId, name));
    }

    private Mono<String> getChampionshipName(Long championshipId) {
        String cached = championshipNameCache.get(championshipId);
        if (cached != null) {
            return Mono.just(cached);
        }

        return championshipService.findById(championshipId)
                .map(Championship::getName)
                .doOnNext(name -> championshipNameCache.put(championshipId, name));
    }

//    -----

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
        entity.setOddsHomeTeam(BaseValues.ODD_BASE_VALUE.getDoubleValue());
        entity.setOddsDraw(BaseValues.ODD_BASE_VALUE.getDoubleValue());
        entity.setOddsVisitingTeam(BaseValues.ODD_BASE_VALUE.getDoubleValue());
        entity.setTotalBetMatch(BaseValues.INITIAL_ZERO.getIntValue());
        entity.setTotalBetHomeTeam(BaseValues.INITIAL_ZERO.getIntValue());
        entity.setTotalBetDraw(BaseValues.INITIAL_ZERO.getIntValue());
        entity.setTotalBetVisitingTeam(BaseValues.INITIAL_ZERO.getIntValue());
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
            return Mono.error(new BusinessException(ErrorMessages.CONFLICT_MATCH_TIME.getMessage()));
        }
        return Mono.just(dto);
    }

    private Mono<Void> validateBatchCloseAdmin(String email) {
        return userService.findByEmail(email)
                .flatMap(user -> {
                    if (!Role.ADMIN.equals(user.getRole())) {
                        log.error("MatchService: FORBIDDEN: User {} without ADMIN role tried batch close at: {}", email, DateTimeConverter.formatInstantNow());
                        return Mono.error(new NotAuthorizedException(ErrorMessages.FORBIDDEN_OPERATION.getMessage()));
                    }
                    return Mono.empty();
                })
                .then();
    }

}
