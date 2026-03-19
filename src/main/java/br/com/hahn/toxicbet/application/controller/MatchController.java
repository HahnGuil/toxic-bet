package br.com.hahn.toxicbet.application.controller;

import br.com.hahn.toxicbet.api.MatchApi;
import br.com.hahn.toxicbet.application.service.MatchEventPublisherService;
import br.com.hahn.toxicbet.application.service.MatchService;
import br.com.hahn.toxicbet.infrastructure.service.JwtService;
import br.com.hahn.toxicbet.model.MatchRequestDTO;
import br.com.hahn.toxicbet.model.MatchResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
public class MatchController extends AbstractController implements MatchApi {

    private final MatchService matchService;
    private final MatchEventPublisherService matchEventPublisherService;

    public MatchController(JwtService jwtService, MatchService matchService, MatchEventPublisherService matchEventPublisherService) {
        super(jwtService);
        this.matchService = matchService;
        this.matchEventPublisherService = matchEventPublisherService;
    }

    @Override
    public Mono<ResponseEntity<MatchResponseDTO>> postCreateMatch(Mono<MatchRequestDTO> matchRequestDTO, ServerWebExchange exchange) {
        return extractUserEmailFromToken(exchange)
                .flatMap(userEmail -> matchService.createMatchDto(matchRequestDTO, userEmail))
                .map(matchResponseDTO -> ResponseEntity.status(HttpStatus.CREATED).body(matchResponseDTO));
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<MatchResponseDTO> streamAllMatches() {
        Flux<MatchResponseDTO> existingMatches = matchService.findAll();
        Flux<MatchResponseDTO> eventStream = matchEventPublisherService.getAllEventsStream();
        return Flux.merge(existingMatches, eventStream)
                .delayElements(Duration.ofSeconds(1));
    }

    @Override
    public Mono<ResponseEntity<Void>> closeMatchForBet(Long matchId, ServerWebExchange exchange) {
        return extractUserEmailFromToken(exchange)
                .flatMap(userEmail -> matchService.closeMatchForBet(matchId, userEmail))
                .then(Mono.just(ResponseEntity.status(HttpStatus.NO_CONTENT).build()));
    }

    @Override
    public Mono<ResponseEntity<Void>> patchCloseMatch(Long matchId, String result, ServerWebExchange exchange) {
        return extractUserEmailFromToken(exchange)
                .flatMap(userEmail -> matchService.closeMatch(matchId, result, userEmail))
                .then(Mono.just(ResponseEntity.status(HttpStatus.NO_CONTENT).build()));
    }

    @Override
    public Mono<ResponseEntity<Void>> patchOpenMatch(Long matchId, ServerWebExchange exchange) {
        return extractUserEmailFromToken(exchange)
                .flatMap(userEmail -> matchService.openMatch(matchId, userEmail))
                .then(Mono.just(ResponseEntity.status(HttpStatus.NO_CONTENT).build()));
    }


    //    TODO - END-POINT APENAS PARA FINS DE TESTES NO K6
    @GetMapping("/{id}")
    public Mono<MatchResponseDTO> getMatchById(@PathVariable Long id){
        return matchService.getById(id)
                .mapNotNull(response ->
                        ResponseEntity.status(HttpStatus.OK).body(response).getBody());
    }
}