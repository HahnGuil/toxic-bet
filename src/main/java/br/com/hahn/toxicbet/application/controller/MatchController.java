package br.com.hahn.toxicbet.application.controller;

import br.com.hahn.toxicbet.api.MatchApi;
import br.com.hahn.toxicbet.application.service.MatchEventPublisherService;
import br.com.hahn.toxicbet.application.service.MatchService;
import br.com.hahn.toxicbet.model.MatchRequestDTO;
import br.com.hahn.toxicbet.model.MatchResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@Slf4j
public class MatchController extends AbstractController implements MatchApi {

    private final MatchService matchService;
    private final MatchEventPublisherService matchEventPublisherService;
    private static final Long STREAM_DELAY_DURATION = 1L;

    @Override
    public Mono<ResponseEntity<MatchResponseDTO>> postCreateMatch(Mono<MatchRequestDTO> matchRequestDTO, ServerWebExchange exchange) {
        return extractUserEmailFromToken(exchange)
                .flatMap(userEmail -> matchService.createMatchDto(matchRequestDTO, userEmail))
                .map(matchResponseDTO -> ResponseEntity.status(HttpStatus.CREATED).body(matchResponseDTO));
    }

    @GetMapping(value = "/match", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<MatchResponseDTO> streamAllMatches() {
        Flux<MatchResponseDTO> existingMatches = matchService.findAll();
        Flux<MatchResponseDTO> eventStream = getEventStream();
        return Flux.merge(existingMatches, eventStream)
                .delayElements(Duration.ofSeconds(STREAM_DELAY_DURATION));
    }

    @GetMapping(value = "/match/find-open", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<MatchResponseDTO> streamOpenBettingMatches(){
       Flux<MatchResponseDTO> openMatches = matchService.findMatchesOpenToBets();

       Flux<MatchResponseDTO> openEventsOnly = getEventStream()
               .filter(matchResponseDTO -> MatchResponseDTO.ResultEnum.OPEN_FOR_BETTING.equals(matchResponseDTO.getResult()));

       return Flux.merge(openMatches, openEventsOnly).delayElements(Duration.ofSeconds(STREAM_DELAY_DURATION));
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

    private Flux<MatchResponseDTO> getEventStream(){
        return matchEventPublisherService.getAllEventsStream();
    }
}