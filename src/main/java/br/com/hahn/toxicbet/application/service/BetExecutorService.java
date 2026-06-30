package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.application.mapper.BetMapper;
import br.com.hahn.toxicbet.domain.exception.BusinessException;
import br.com.hahn.toxicbet.domain.exception.ConflictException;
import br.com.hahn.toxicbet.domain.model.Match;
import br.com.hahn.toxicbet.domain.model.enums.Result;
import br.com.hahn.toxicbet.domain.model.enums.ErrorMessages;
import br.com.hahn.toxicbet.domain.repository.BetRepository;
import br.com.hahn.toxicbet.model.BetRequestDTO;
import br.com.hahn.toxicbet.model.BetResponseDTO;
import br.com.hahn.toxicbet.util.DateTimeConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Service responsible for executing bet operations.
 * This service performs the actual bet processing logic after being
 * enqueued by BetProcessorService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BetExecutorService {

    private final BetRepository betRepository;
    private final UserService userService;
    private final MatchService matchService;
    private final BetMapper mapper;
    private final OddsService oddsService;
    private final TransactionalOperator transactionalOperator;
    private final BetProcessingMetrics metrics;
    private final MatchEventPublisherService matchEventPublisherService;

    public Mono<Void> processBet(BetProcessorService.BetRequest betRequest) {
        Long matchId = betRequest.betRequestDTO().getMatchId();
        BetProcessingMetrics.ProcessingSample sample = metrics.recordProcessingStarted(matchId);

        log.debug("BetExecutorService: Processing bet for match {} by user {}",
                matchId, betRequest.userEmail());

        return createBetResponse(betRequest.betRequestDTO(), betRequest.userEmail())
                .doOnSuccess(response -> {
                    metrics.recordProcessingCompleted(matchId, sample);
                    log.info("BetExecutorService: Bet processed successfully for match {}. Response: {}", matchId, response);
                    betRequest.responseSink().success(response);
                })
                .doOnError(error -> {
                    metrics.recordProcessingFailed(matchId, error);
                    if (isExpectedBetError(error)) {
                        log.debug("BetExecutorService: Bet rejected for match {}: {}", matchId, error.getMessage());
                    } else {
                        log.error("BetExecutorService: Error processing bet for match {}: {}", matchId, error.getMessage(), error);
                    }
                    betRequest.responseSink().error(error);
                })
                .then()
                .onErrorResume(error -> {
                    log.debug("BetExecutorService: Error handled, continuing to next bet");
                    return Mono.empty();
                });
    }

    private Mono<Match> isMatchOpenForBetting(Long matchId) {
        return matchService.findById(matchId)
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("BetExecutorService: NOT_FOUND: Match for id: {}. Throw Not Found Exception at: {}",
                            matchId, DateTimeConverter.formatInstantNow());
                    return Mono.error(new BusinessException(ErrorMessages.MATCH_NOT_OPEN_TO_BETS.getMessage()));
                }));
    }

    private Mono<BetResponseDTO> createBetResponse(BetRequestDTO dto, String userEmail) {
        return Mono.zip(
                findUsersByEmail(userEmail),
                isMatchOpenForBetting(dto.getMatchId())
        ).flatMap(tuple -> createAndSaveBet(dto, tuple.getT1(), tuple.getT2()));
    }

    private Mono<UUID> findUsersByEmail(String userEmail) {
        return userService.findUserByEmail(userEmail);
    }

    private Mono<BetResponseDTO> createAndSaveBet(BetRequestDTO dto, UUID userID, Match match) {
        var bet = mapper.toEntity(dto, userID);
        bet.setBetOdds(resolveBetOdds(match, bet.getResult()));
        bet.setUserPoint(null); // Será calculado quando fechar a partida

        return betRepository.existsByUserIdAndMatchId(userID, dto.getMatchId())
                .flatMap(alreadyPlaced -> {
                    if (Boolean.TRUE.equals(alreadyPlaced)) {
                        return Mono.error(new ConflictException(ErrorMessages.BET_ALREADY_PLACED.getMessage()));
                    }
                    return oddsService.updateOddsForBet(bet.getMatchId(), bet.getResult())
                            .then(betRepository.save(bet))
                            .map(mapper::toDTO)
                            .as(transactionalOperator::transactional);
                })
                .onErrorMap(DuplicateKeyException.class,
                        error -> new ConflictException(ErrorMessages.BET_ALREADY_PLACED.getMessage()))
                .flatMap(response -> publishOddsUpdate(dto.getMatchId()).thenReturn(response));
    }

    private Double resolveBetOdds(Match match, Result result) {
        return switch (result) {
            case HOME_WIN -> match.getOddsHomeTeam();
            case DRAW -> match.getOddsDraw();
            case VISITING_WIN -> match.getOddsVisitingTeam();
            default -> null;
        };
    }

    private Mono<Void> publishOddsUpdate(Long matchId) {
        return matchService.findById(matchId)
                .flatMap(matchService::buildMatchResponseDTO)
                .doOnNext(matchEventPublisherService::publishOddsUpdate)
                .then();
    }

    private boolean isExpectedBetError(Throwable error) {
        return error instanceof BusinessException || error instanceof ConflictException;
    }
}
