package br.com.hahn.toxicbet.application.controller;

import br.com.hahn.toxicbet.api.BetApi;
import br.com.hahn.toxicbet.application.service.BetService;
import br.com.hahn.toxicbet.application.service.MatchEventPublisherService;
import br.com.hahn.toxicbet.application.service.MatchService;
import br.com.hahn.toxicbet.infrastructure.service.JwtService;
import br.com.hahn.toxicbet.model.BetRequestDTO;
import br.com.hahn.toxicbet.model.BetResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class BetController extends AbstractController implements BetApi {

    private final BetService betService;
    private final MatchService matchService;
    private final MatchEventPublisherService matchEventPublisherService;

    public BetController(JwtService jwtService, BetService betService,
                         MatchService matchService, MatchEventPublisherService matchEventPublisherService) {
        super(jwtService);
        this.betService = betService;
        this.matchService = matchService;
        this.matchEventPublisherService = matchEventPublisherService;
    }

    @Override
    public Mono<ResponseEntity<BetResponseDTO>> postRegisterBet(Mono<BetRequestDTO> betRequestDTO, ServerWebExchange exchange) {
        return extractUserIdFromToken(exchange)
                .flatMap(userEmail -> betService.placeBet(betRequestDTO, userEmail))
                .flatMap(betResponse ->
                        matchService.findById(betResponse.getMatchId())
                                .flatMap(matchService::buildMatchResponseDTO)
                                .doOnSuccess(matchEventPublisherService::publishOddsUpdate)
                                .thenReturn(betResponse)
                )
                .map(dto -> ResponseEntity.status(HttpStatus.CREATED).body(dto));
    }
}

