package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.application.mapper.BetMapper;
import br.com.hahn.toxicbet.domain.exception.BusinessException;
import br.com.hahn.toxicbet.domain.model.Match;
import br.com.hahn.toxicbet.domain.model.enums.ErrorMessages;
import br.com.hahn.toxicbet.domain.repository.BetRepository;
import br.com.hahn.toxicbet.model.BetRequestDTO;
import br.com.hahn.toxicbet.model.BetResponseDTO;
import br.com.hahn.toxicbet.util.DateTimeConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
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
    private final TransactionalOperator transactionalOperator;

    public Mono<BetResponseDTO> placeBet(Mono<BetRequestDTO> betRequestDTOMono, String userEmail) {
        return betRequestDTOMono
                .flatMap(dto -> createBetResponse(dto, userEmail));
    }

    private Mono<Match> isMatchOpenForBetting(Long matchId){
        return matchService.findById(matchId)
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("MatchService: NOT_FOUND: Match for id: {}. Throw Not Found Exception at: {}", matchId, DateTimeConverter.formatInstantNow());
                    return Mono.error(new BusinessException(ErrorMessages.MATCH_NOT_OPEN_TO_BETS.getMessage()));
                }));
    }

    private Mono<BetResponseDTO> createBetResponse(BetRequestDTO dto, String userEmail){
        return Mono.zip(
                findUsersByEmail(userEmail),
                isMatchOpenForBetting(dto.getMatchId())
        ).flatMap(tuple -> createAndSaveBet(dto, tuple.getT1()));
    }

    private Mono<UUID> findUsersByEmail(String userEmail){
        return userService.findUserByEmail(userEmail);
    }

    private Mono<BetResponseDTO> createAndSaveBet(BetRequestDTO dto, UUID userID){
        var bet = mapper.toEntity(dto, userID);

        return oddsService.updateOddsForBet(bet.getMatchId(), bet.getResult(), dto.getOdds())
                .flatMap(userPoints -> {
                    bet.setUserPoint(userPoints);
                    bet.setBetOdds(dto.getOdds());
                    return betRepository.save(bet);
                })
                .map(mapper::toDTO)
                .as(transactionalOperator::transactional);
    }
}