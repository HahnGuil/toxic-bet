package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.application.mapper.BetMapper;
import br.com.hahn.toxicbet.domain.exception.MatchNotOpenForBettingException;
import br.com.hahn.toxicbet.domain.model.Match;
import br.com.hahn.toxicbet.domain.model.enums.ErrorMessages;
import br.com.hahn.toxicbet.domain.model.enums.Result;
import br.com.hahn.toxicbet.domain.repository.BetRepository;
import br.com.hahn.toxicbet.model.BetRequestDTO;
import br.com.hahn.toxicbet.model.BetResponseDTO;
import br.com.hahn.toxicbet.util.DateTimeConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Service class responsible for handling bet-related operations.
 *
 * <p>
 * This class provides methods to process bets, verify match statuses,
 * retrieve user details, and manage the creation and saving of bets
 * in a reactive programming flow.
 * </p>
 *
 * <p>
 * Dependencies:
 * <ul>
 *     <li>{@link BetRepository} for accessing and persisting bet data</li>
 *     <li>{@link UserService} for retrieving user information</li>
 *     <li>{@link MatchService} for fetching match details</li>
 *     <li>{@link BetMapper} for mapping between DTOs and entities</li>
 * </ul>
 * </p>
 *
 * <p>
 * This service uses Project Reactor's {@link Mono} to handle asynchronous
 * and non-blocking operations.
 * </p>
 *
 * @author HahnGuil
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BetService {

    private final BetRepository betRepository;
    private final UserService userService;
    private final MatchService matchService;
    private final BetMapper mapper;

    /**
     * Processes a bet request in a reactive flow.
     *
     * <p>
     * This method performs the following steps:
     * <ol>
     *     <li>Logs the start of the bet processing with the user's email and current timestamp</li>
     *     <li>Unwraps the {@link Mono} to access the bet request data</li>
     *     <li>Delegates to {@link #createBetResponse(BetRequestDTO, String)} to validate and create the bet response</li>
     * </ol>
     * </p>
     *
     * @param betRequestDTOMono a {@link Mono} containing the {@link BetRequestDTO} with the bet details
     * @param userEmail the email of the user placing the bet
     * @return a {@link Mono} emitting the {@link BetResponseDTO} with the processed bet details
     *
     * @author HahnGuil
     */
    public Mono<BetResponseDTO> placeBet(Mono<BetRequestDTO> betRequestDTOMono, String userEmail) {
        return betRequestDTOMono
                .doOnNext(dto -> log.info("BetService: Starting placeBet for user with email: {} at: {}",
                        userEmail, DateTimeConverter.formatInstantNow()))
                .flatMap(dto -> createBetResponse(dto, userEmail));
    }

    /**
     * Verifies if a match is open for betting in a reactive flow.
     *
     * <p>
     * This method performs the following steps:
     * <ol>
     *     <li>Logs the start of the verification process with the current timestamp</li>
     *     <li>Fetches the match details using the provided match ID</li>
     *     <li>Checks if the match result is {@link Result#OPEN_FOR_BETTING}</li>
     *     <li>If the match is not open for betting, logs an error and throws a {@link MatchNotOpenForBettingException}</li>
     *     <li>Returns the match details if the match is open for betting</li>
     * </ol>
     * </p>
     *
     * @author HahnGuil
     * @param matchId the ID of the match to be verified
     * @return a {@link Mono} emitting the {@link Match} if it is open for betting
     * @throws MatchNotOpenForBettingException if the match is not open for betting
     */
    private Mono<Match> isMatchOpenForBetting(Long matchId){
        log.info("BetService: Checking if the match is already open for betting at: {}", DateTimeConverter.formatInstantNow());
        return matchService.findById(matchId)
                .flatMap(match -> {
                    if(match.getResult() != Result.OPEN_FOR_BETTING){
                        log.error("BetService: Match is not open to betting. Throw the MatchNotOpenForBettingException ar: {}", DateTimeConverter.formatInstantNow());
                        return Mono.error(new MatchNotOpenForBettingException(ErrorMessages.MATCH_NOT_OPEN_TO_BETS.getMessage()));
                    }
                    return Mono.just(match);
                });
    }

    /**
     * Creates a bet response in a reactive flow.
     *
     * <p>
     * This method performs the following steps:
     * <ol>
     *     <li>Logs the creation of the bet response with the current timestamp</li>
     *     <li>Fetches the user details by their email</li>
     *     <li>Verifies if the match is open for betting</li>
     *     <li>Combines the user details and match verification results</li>
     *     <li>Delegates to {@link #createAndSaveBet(BetRequestDTO, UUID)} to save the bet and generate the response</li>
     * </ol>
     * </p>
     * @author HahnGuil
     * @param dto the {@link BetRequestDTO} containing the bet details
     * @param userEmail the email of the user placing the bet
     * @return a {@link Mono} emitting the {@link BetResponseDTO} with the created bet details
     */
    private Mono<BetResponseDTO> createBetResponse(BetRequestDTO dto, String userEmail){
        log.info("BetService: Create Bet response at: {}", DateTimeConverter.formatInstantNow());
        return Mono.zip(
                findUsersByEmail(userEmail),
                isMatchOpenForBetting(dto.getMatchId())
        ).flatMap(tuple -> createAndSaveBet(dto, tuple.getT1()));
    }

    /**
     * Finds a user by their email in a reactive flow.
     *
     * <p>
     * This method performs the following steps:
     * <ol>
     *     <li>Logs the start of the user search process with the provided email and current timestamp</li>
     *     <li>Delegates to {@link UserService#findUserByEmail(String)} to retrieve the user ID</li>
     * </ol>
     * </p>
     *
     * @author HahnGuil
     * @param userEmail the email of the user to be found
     * @return a {@link Mono} emitting the {@link UUID} of the user
     */
    private Mono<UUID> findUsersByEmail(String userEmail){
        log.info("BetService: Find user by email: {}, at: {}", userEmail, DateTimeConverter.formatInstantNow());
        return userService.findUserByEmail(userEmail);
    }

    /**
     * Creates and saves a bet in a reactive flow.
     *
     * <p>
     * This method performs the following steps:
     * <ol>
     *     <li>Logs the start of the bet saving process with the current timestamp</li>
     *     <li>Converts the {@link BetRequestDTO} and user ID into a bet entity</li>
     *     <li>Saves the bet entity to the repository</li>
     *     <li>Maps the saved entity to a {@link BetResponseDTO}</li>
     * </ol>
     * </p>
     *
     * @author HahnGuiL
     * @param dto the {@link BetRequestDTO} containing the bet details
     * @param userID the {@link UUID} of the user placing the bet
     * @return a {@link Mono} emitting the {@link BetResponseDTO} with the saved bet details
     */
    private Mono<BetResponseDTO> createAndSaveBet(BetRequestDTO dto, UUID userID){
        log.info("BetService: Save Bet at: {}", DateTimeConverter.formatInstantNow());
        var bet = mapper.toEntity(dto, userID);
        return betRepository.save(bet)
                .map(mapper::toDTO);
    }
}