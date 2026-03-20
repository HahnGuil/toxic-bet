package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.domain.exception.BusinessException;
import br.com.hahn.toxicbet.domain.model.enums.ErrorMessages;
import br.com.hahn.toxicbet.domain.model.enums.Result;
import br.com.hahn.toxicbet.model.BetRequestDTO;
import br.com.hahn.toxicbet.model.BetResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class BetService {

    private final BetProcessorService betProcessorService;
    private final MatchService matchService;

    public Mono<BetResponseDTO> placeBet(Mono<BetRequestDTO> betRequestDTOMono, String userEmail) {
        return betRequestDTOMono
                .flatMap(dto -> isMatchOpenToBet(dto.getMatchId()).thenReturn(dto))
                .flatMap(dto -> {
                    log.debug("BetService: Placing bet for match {} by user {}", dto.getMatchId(), userEmail);
                    return betProcessorService.enqueueBet(dto, userEmail);
                });
    }

    private Mono<Void> isMatchOpenToBet(Long matchId) {
        return matchService.findById(matchId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorMessages.MATCH_NOT_FOUND.getMessage())))
                .flatMap(match -> {
                    Result result = match.getResult();
                    if (result != Result.OPEN_FOR_BETTING) {
                        return Mono.error(new BusinessException(ErrorMessages.MATCH_NOT_OPEN_TO_BETS.getMessage()));
                    }
                    return Mono.empty();
                });
    }
}