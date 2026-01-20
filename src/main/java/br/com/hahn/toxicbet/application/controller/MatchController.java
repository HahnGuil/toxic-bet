package br.com.hahn.toxicbet.application.controller;

import br.com.hahn.toxicbet.api.MatchApi;
import br.com.hahn.toxicbet.application.service.MatchService;
import br.com.hahn.toxicbet.model.MatchRequestDTO;
import br.com.hahn.toxicbet.model.MatchResponseDTO;
import br.com.hahn.toxicbet.model.UpdateScoreRequestDTO;
import br.com.hahn.toxicbet.util.DateTimeConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;

@RestController
@RequestMapping("/match")
public class MatchController implements MatchApi {

    private final Sinks.Many<MatchResponseDTO> matchSink;

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
        this.matchSink = Sinks.many().multicast().onBackpressureBuffer();
    }

    @Override
    public Mono<ResponseEntity<MatchResponseDTO>> postCreateMatch(Mono<MatchRequestDTO> matchRequestDTO, ServerWebExchange exchange) {
        return DateTimeConverter.formatInstantNowReactive()
                .then(matchService.createMatchDto(matchRequestDTO))
                .doOnSuccess(matchSink::tryEmitNext)
                .map(dto -> ResponseEntity.status(HttpStatus.CREATED).body(dto));
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<MatchResponseDTO> streamAllMatches() {
        Flux<MatchResponseDTO> matches = matchService.findAll();
        return Flux.merge(matches, matchSink.asFlux())
                .delayElements(Duration.ofSeconds(4));
    }

    @Override
    public Mono<ResponseEntity<MatchResponseDTO>> patchUpdateMatchScore(Long matchId, Mono<UpdateScoreRequestDTO> updateScoreRequestDTO, ServerWebExchange exchange) {
        return DateTimeConverter.formatInstantNowReactive()
                .then(updateScoreRequestDTO)
                .flatMap(dto -> matchService.updateMatchScore(
                        matchId,
                        dto.getHomeTeamScore(),
                        dto.getVisitingTeamScore()
                ))
                .doOnSuccess(matchSink::tryEmitNext)
                .map(ResponseEntity::ok);
    }

    @GetMapping(value = "/scores", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<MatchResponseDTO> getStreamScores(){
        Flux<MatchResponseDTO> matches = matchService.findAllInProgress();
        return Flux.merge(matches, matchSink.asFlux())
                .delayElements(Duration.ofSeconds(1));
    }
}