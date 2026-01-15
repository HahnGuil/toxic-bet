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

@Service
@RequiredArgsConstructor
@Slf4j
public class BetService {

    private final BetRepository betRepository;
    private final UserService userService;
    private final MatchService matchService;
    private final BetMapper mapper;
    private final OddsService oddsService;

    public Mono<BetResponseDTO> placeBet(Mono<BetRequestDTO> betRequestDTOMono, String userEmail) {
        return betRequestDTOMono
                .doOnNext(dto -> log.info("BetService: Starting placeBet for user with email: {} at: {}",
                        userEmail, DateTimeConverter.formatInstantNow()))
                .flatMap(dto -> createBetResponse(dto, userEmail));
    }

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

    private Mono<BetResponseDTO> createBetResponse(BetRequestDTO dto, String userEmail){
        log.info("BetService: Create Bet response at: {}", DateTimeConverter.formatInstantNow());
        return Mono.zip(
                findUsersByEmail(userEmail),
                isMatchOpenForBetting(dto.getMatchId())
        ).flatMap(tuple -> createAndSaveBet(dto, tuple.getT1()));
    }

    private Mono<UUID> findUsersByEmail(String userEmail){
        log.info("BetService: Find user by email: {}, at: {}", userEmail, DateTimeConverter.formatInstantNow());
        return userService.findUserByEmail(userEmail);
    }

    private Mono<BetResponseDTO> createAndSaveBet(BetRequestDTO dto, UUID userID){
        log.info("BetService: Save Bet at: {}", DateTimeConverter.formatInstantNow());
        var bet = mapper.toEntity(dto, userID);
        return betRepository.save(bet)
                .flatMap(savedBet ->
                        oddsService.updateOddsForBet(savedBet.getMatchId(), savedBet.getResult())
                                .thenReturn(savedBet))
                .map(mapper::toDTO);
    }
}