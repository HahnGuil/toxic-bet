package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.application.mapper.MatchMapper;
import br.com.hahn.toxicbet.domain.exception.*;
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

/**
 * Service responsible for managing operations related to matches.
 * Includes functionalities such as creation, score updates, validations, and match retrieval.
 *
 * @author HahnGuil
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MatchService {

    private final TeamService teamService;
    private final MatchRepository repository;
    private final MatchMapper mapper;

    private static final Integer INITIAL_SCORE = 0;

    /**
     * Creates a match based on a request DTO.
     * Performs validations before creating the match.
     *
     * @author HahnGuil
     * @param matchRequestDTOMono Mono containing the match request DTO
     * @return Mono containing the created match response DTO
     */
    public Mono<MatchResponseDTO> createMatchDto(Mono<MatchRequestDTO> matchRequestDTOMono) {
        return matchRequestDTOMono
                .flatMap(this::isMatchTimeValid)
                .flatMap(this::validatesTeamNotPlayingAtTheScheduledTime)
                .flatMap(this::validateTeamsAreDifferent)
                .flatMap(this::fetchTeamsAndCreateMatch);
    }

    /**
     * Retrieves all matches without score.
     *
     * @author HahnGuil
     * @return Flux containing all match response DTOs
     */
    public Flux<MatchResponseDTO> findAll(){
        log.info("MatchService: Find all matches at: {}", DateTimeConverter.formatInstantNow());
        return repository.findAll().flatMap(this::buildMatchResponseDTO);
    }

    /**
     * Updates match status to "IN_PROGRESS" if the match time has passed.
     *
     * @author HahnGuil
     * @return Mono containing the number of updated matches
     */
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

    /**
     * Updates the score of a specific match.
     *
     * @author HahnGuil
     * @param matchId ID of the match to be updated
     * @param homeScore New score for the home team
     * @param visitingScore New score for the visiting team
     * @return Mono containing the updated match response DTO
     */
    public Mono<MatchResponseDTO> updateMatchScore(Long matchId, Integer homeScore, Integer visitingScore) {
        log.info("MatchService: Updating match {} score at: {}", matchId, DateTimeConverter.formatInstantNow());
        return repository.findById(matchId)
                .switchIfEmpty(Mono.error(new MatchNotFoundException(ErrorMessages.MATCH_NOT_FOUND.getMessage() + matchId)))
                .flatMap(match -> validateAndUpdateScore(match, homeScore, visitingScore))
                .flatMap(repository::save)
                .flatMap(this::buildMatchResponseDTO);
    }

    /**
     * Retrieves all matches that are currently in progress.
     *
     * @author HahnGuil
     * @return Flux containing the response DTOs of matches in progress
     */
    public Flux<MatchResponseDTO> findAllInProgress() {
        log.info("MatchService: Finding all matches IN_PROGRESS at: {}", DateTimeConverter.formatInstantNow());
        return repository.findByResult(Result.IN_PROGRESS)
                .flatMap(this::buildMatchResponseDTO);
    }

    /**
     * Retrieves a match by its ID.
     *
     * <p>
     * This method performs the following steps:
     * <ol>
     *     <li>Logs the start of the match retrieval process with the provided ID and current timestamp.</li>
     *     <li>Attempts to find the match in the repository by its ID.</li>
     *     <li>If an error occurs during the retrieval, logs the error and throws a {@link MatchNotFoundException}.</li>
     *     <li>If no match is found, returns a {@link Mono} error with a {@link MatchNotFoundException}.</li>
     * </ol>
     * </p>
     *
     * @author HahnGuil
     * @param id the ID of the match to be retrieved
     * @return a {@link Mono} emitting the {@link Match} if found, or an error if not found
     */
    public Mono<Match> findById(Long id){
        log.info("MatchService: Find match by id: {}, at: {}", id, DateTimeConverter.formatInstantNow());
        return repository.findById(id)
                .doOnError(error -> log.info("MatchService: Match not found to this id: {}. Throw the MatchNotFoudException at: {}", id, DateTimeConverter.formatInstantNow()))
                .switchIfEmpty(Mono.error(new MatchNotFoundException(ErrorMessages.MATCH_NOT_FOUND.getMessage())));
    }

    /**
     * Fetches the teams and creates a new match.
     *
     * @author HahnGuil
     * @param dto Match request DTO
     * @return Mono containing the created match response DTO
     */
    private Mono<MatchResponseDTO> fetchTeamsAndCreateMatch(MatchRequestDTO dto) {
        log.info("MatchService: Fetch teams and create match at: {}", DateTimeConverter.formatInstantNow());
        return Mono.zip(
                findTeamWithContext(dto.getHomeTeamId(), ErrorMessages.HOME_TEAM_NOT_FOUND),
                findTeamWithContext(dto.getVisitingTeamId(), ErrorMessages.VISITING_TEAM_NOT_FOUND)
        ).flatMap(teams -> createAndSaveMatch(dto, teams.getT1(), teams.getT2()));
    }

    /**
     * Finds a team by ID and throws a custom exception if not found.
     *
     * @author HahnGuil
     * @param teamId ID of the team to find
     * @param errorMessage Error message to use in the exception
     * @return Mono containing the found team
     */
    private Mono<Team> findTeamWithContext(Long teamId, ErrorMessages errorMessage) {
        log.info("MatchService: Finding team with id: {} at: {}", teamId, DateTimeConverter.formatInstantNow());
        return teamService.findById(teamId)
                .doOnSuccess(team -> log.info("MatchService: Team {} found successfully at: {}", teamId, DateTimeConverter.formatInstantNow()))
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("MatchService: NOT_FOUND: Team {} not found at: {}", teamId, DateTimeConverter.formatInstantNow());
                    return Mono.error(new TeamNotFoundException(errorMessage.getMessage()));
                }));
    }

    /**
     * Creates and saves a new match.
     *
     * @author HahnGuil
     * @param dto Match request DTO
     * @param homeTeam Home team
     * @param visitingTeam Visiting team
     * @return Mono containing the created match response DTO
     */
    private Mono<MatchResponseDTO> createAndSaveMatch(MatchRequestDTO dto, Team homeTeam, Team visitingTeam) {
        log.info("MatchService: Create and Save match, at: {}", DateTimeConverter.formatInstantNow());
        var entity = prepareMatchEntity(dto);
        return repository.save(entity)
                .map(savedMatch -> mapper.toDto(savedMatch, homeTeam.getName(), visitingTeam.getName()));
    }

    /**
     * Prepares the match entity based on the request DTO.
     *
     * @author HahnGuil
     * @param dto Match request DTO
     * @return Prepared match entity
     */
    private Match prepareMatchEntity(MatchRequestDTO dto) {
        log.info("MatchService: convert dto to entity at: {}", DateTimeConverter.formatInstantNow());
        var entity = mapper.toEntity(dto);
        entity.setResult(Result.NOT_STARTED);
        entity.setHomeTeamScore(INITIAL_SCORE);
        entity.setVisitingTeamScore(INITIAL_SCORE);
        return entity;
    }

    /**
     * Validates if the match time is valid (not in the past).
     *
     * @author HahnGuil
     * @param dto Match request DTO
     * @return Mono containing the validated DTO or an error if invalid
     */
    private Mono<MatchRequestDTO> isMatchTimeValid(MatchRequestDTO dto) {
        log.info("MatchService: Validating match time at: {}", DateTimeConverter.formatInstantNow());
        var matchTime = DateTimeConverter.parseToLocalDateTime(dto.getMatchTime());
        assert matchTime != null;
        if (matchTime.isBefore(LocalDateTime.now())) {
            log.error("MatchService: UNPROCESSABLE_ENTITY: Invalid match time, throw InvalidMatchTimeException at: {}", DateTimeConverter.formatInstantNow());
            return Mono.error(new InvalidMatchTimeException(ErrorMessages.INVALID_MATCH_TIME.getMessage()));
        }
        return Mono.just(dto);
    }

    /**
     * Validates if the teams in the match are different.
     *
     * @author HahnGuil
     * @param dto Match request DTO
     * @return Mono containing the validated DTO or an error if teams are the same
     */
    private Mono<MatchRequestDTO> validateTeamsAreDifferent(MatchRequestDTO dto) {
        log.info("MatchService: Validating teams are different at: {}", DateTimeConverter.formatInstantNow());
        if (dto.getHomeTeamId().equals(dto.getVisitingTeamId())) {
            log.error("MatchService: UNPROCESSABLE_ENTITY: Team are not different. Throw InvalidTeamException at: {}", DateTimeConverter.formatInstantNow());
            return Mono.error(new InvalidTeamException(ErrorMessages.TEAM_MUST_BE_DIFFERENT.getMessage()));
        }
        return Mono.just(dto);
    }

    /**
     * Validates if the teams are not involved in other matches at the same time.
     *
     * @author HahnGuil
     * @param dto Match request DTO
     * @return Mono containing the validated DTO or an error if there is a conflict
     */
    private Mono<MatchRequestDTO> validatesTeamNotPlayingAtTheScheduledTime(MatchRequestDTO dto){
        log.info("MatchService: Validating team not playing at the scheduled time: {} at: {}", dto.getMatchTime(), DateTimeConverter.formatInstantNow());
        var matchTime = DateTimeConverter.parseToLocalDateTime(dto.getMatchTime());
        assert matchTime != null;
        return hasTeamConflictInTimeWindow(dto, matchTime)
                .flatMap(hasConflict -> handleConflictResult(hasConflict, dto));
    }

    /**
     * Checks if there is a team conflict within a specific time window.
     *
     * @author HahnGuil
     * @param dto Match request DTO
     * @param matchTime Scheduled match time
     * @return Mono containing true if there is a conflict, false otherwise
     */
    private Mono<Boolean> hasTeamConflictInTimeWindow(MatchRequestDTO dto, LocalDateTime matchTime) {
        log.info("MatchService: Initiating validation to check if teams are already involved in other matches within the same time window. Home Team: {}, Visiting Team: {}, at: {}", dto.getHomeTeamId(), dto.getVisitingTeamId(), DateTimeConverter.formatInstantNow());
        var startWindow = matchTime.minusHours(3);
        var endWindow = matchTime.plusHours(3);

        return repository.findAll()
                .filter(match -> isTeamInvolved(match, dto))
                .filter(match -> isInTimeWindow(match.getMatchTime(), startWindow, endWindow))
                .hasElements();
    }

    /**
     * Checks if any team from the DTO is involved in the given match.
     *
     * @author HahnGuil
     * @param match Match to check
     * @param dto Match request DTO
     * @return true if a team is involved, false otherwise
     */
    private boolean isTeamInvolved(Match match, MatchRequestDTO dto) {
        log.info("MatchService: Validating time windows for teams at: {}", DateTimeConverter.formatInstantNow());
        return isHomeTeamInvolved(match, dto) || isVisitingTeamInvolved(match, dto);
    }

    /**
     * Checks if the home team from the DTO is involved in the given match.
     *
     * @author HahnGuil
     * @param match Match to check
     * @param dto Match request DTO
     * @return true if the home team is involved, false otherwise
     */
    private boolean isHomeTeamInvolved(Match match, MatchRequestDTO dto) {
        log.info("MatchService: validating if home team: {} is involved at: {}", dto.getHomeTeamId(), DateTimeConverter.formatInstantNow());
        return match.getHomeTeamId().equals(dto.getHomeTeamId())
                || match.getVisitingTeamId().equals(dto.getHomeTeamId());
    }

    /**
     * Checks if the visiting team from the DTO is involved in the given match.
     *
     * @author HahnGuil
     * @param match Match to check
     * @param dto Match request DTO
     * @return true if the visiting team is involved, false otherwise
     */
    private boolean isVisitingTeamInvolved(Match match, MatchRequestDTO dto) {
        log.info("MatchService: validating if visiting team: {} is involved at: {}", dto.getVisitingTeamId(), DateTimeConverter.formatInstantNow());
        return match.getHomeTeamId().equals(dto.getVisitingTeamId())
                || match.getVisitingTeamId().equals(dto.getVisitingTeamId());
    }

    /**
     * Checks if a match time falls within a specific time window.
     *
     * @author HahnGuil
     * @param matchTime Match time to check
     * @param startWindow Start of the time window
     * @param endWindow End of the time window
     * @return true if the match time is within the window, false otherwise
     */
    private boolean isInTimeWindow(LocalDateTime matchTime, LocalDateTime startWindow, LocalDateTime endWindow) {
        log.info("MatchService: validating the time windows for teams that are involved at the match at: {}", DateTimeConverter.formatInstantNow());
        return (matchTime.isAfter(startWindow) || matchTime.isEqual(startWindow))
                && (matchTime.isBefore(endWindow) || matchTime.isEqual(endWindow));
    }

    /**
     * Handles the result of the conflict validation.
     *
     * @author HahnGuil
     * @param hasConflict Indicates if there is a conflict
     * @param dto Match request DTO
     * @return Mono containing the DTO if there is no conflict, or an error if there is
     */
    private Mono<MatchRequestDTO> handleConflictResult(boolean hasConflict, MatchRequestDTO dto) {
        log.info("MatchService: Validated conflict at scheduler fot team at: {}", DateTimeConverter.formatInstantNow());
        if (hasConflict) {
            log.error("MathService: CONFLICT: Team already involved at match. Throw ConflictMatchTimeException at: {}", DateTimeConverter.formatInstantNow());
            return Mono.error(new ConflictMatchTimeException(ErrorMessages.CONFLICT_MATCH_TIME.getMessage()));
        }
        return Mono.just(dto);
    }

    /**
     * Validates and updates the score of a match.
     *
     * @author HahnGuil
     * @param match Match to be updated
     * @param homeScore New score for the home team
     * @param visitingScore New score for the visiting team
     * @return Mono containing the updated match
     */
    private Mono<Match> validateAndUpdateScore(Match match, Integer homeScore, Integer visitingScore) {
        log.info("MatchService: Check and update Score for match: {}, at: {}", match.getId(), DateTimeConverter.formatInstantNow());
        if (match.getResult() != Result.IN_PROGRESS) {
            log.error("MatchServe: UNPROCESSABLE_ENTITY: Cannot update match: {}, Throw InvalidMatchStateException at: {}", match.getId(), DateTimeConverter.formatInstantNow());
            return Mono.error(new InvalidMatchStateException(ErrorMessages.CAN_NOT_UPDATE_MATCH.getMessage()));
        }

        match.setHomeTeamScore(homeScore);
        match.setVisitingTeamScore(visitingScore);

        return Mono.just(match);
    }

    /**
     * Builds a match response DTO from a match entity.
     *
     * @author HahnGuil
     * @param match Match entity
     * @return Mono containing the match response DTO
     */
    private Mono<MatchResponseDTO> buildMatchResponseDTO(Match match) {
        log.info("MatchService: Build Response DTO at: {}", DateTimeConverter.formatInstantNow());
        return Mono.zip(
                teamService.findById(match.getHomeTeamId()),
                teamService.findById(match.getVisitingTeamId())
        ).map(teams -> mapper.toDto(match, teams.getT1().getName(), teams.getT2().getName()));
    }
}