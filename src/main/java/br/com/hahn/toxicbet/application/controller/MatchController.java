package br.com.hahn.toxicbet.application.controller;

import br.com.hahn.toxicbet.api.MatchApi;
import br.com.hahn.toxicbet.application.service.MatchEventPublisherService;
import br.com.hahn.toxicbet.application.service.MatchService;
import br.com.hahn.toxicbet.model.MatchRequestDTO;
import br.com.hahn.toxicbet.model.MatchResponseDTO;
import br.com.hahn.toxicbet.model.UpdateScoreRequestDTO;
import br.com.hahn.toxicbet.model.UserRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequestMapping("/match")
@RequiredArgsConstructor
public class MatchController implements MatchApi {

    private final MatchService matchService;
    private final MatchEventPublisherService matchEventPublisherService;



    @Override
    public Mono<ResponseEntity<MatchResponseDTO>> postCreateMatch(Mono<MatchRequestDTO> matchRequestDTO, ServerWebExchange exchange) {
        return matchService.createMatchDto(matchRequestDTO)
                .doOnSuccess(matchEventPublisherService::publishMatchCreated)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<MatchResponseDTO> streamAllMatches() {
        Flux<MatchResponseDTO> existingMatches = matchService.findAll();

        Flux<MatchResponseDTO> eventStream = matchEventPublisherService.getAllEventsStream();

        return Flux.merge(existingMatches, eventStream)
                .delayElements(Duration.ofSeconds(1));
    }

//    TODO - END-POINT APENAS PARA FINS DE TESTES NO K6
    @GetMapping("/{id}")
    public Mono<MatchResponseDTO> getMatchById(@PathVariable Long id){
        return matchService.getById(id)
                .mapNotNull(response ->
                        ResponseEntity.status(HttpStatus.OK).body(response).getBody());
    }

    @Override
    public Mono<ResponseEntity<Void>> patchCloseMatch(Long matchId, String result, ServerWebExchange exchange) {
        return matchService.closeMatch(matchId, result)
                .then(Mono.just(ResponseEntity.status(HttpStatus.NO_CONTENT).build()));
    }
}