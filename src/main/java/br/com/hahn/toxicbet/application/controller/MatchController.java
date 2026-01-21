package br.com.hahn.toxicbet.application.controller;

import br.com.hahn.toxicbet.api.MatchApi;
import br.com.hahn.toxicbet.application.service.MatchEventPublisherService;
import br.com.hahn.toxicbet.application.service.MatchService;
import br.com.hahn.toxicbet.model.MatchRequestDTO;
import br.com.hahn.toxicbet.model.MatchResponseDTO;
import br.com.hahn.toxicbet.model.UpdateScoreRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
        Flux<MatchResponseDTO> matches = matchService.findAll();
        return Flux.merge(matches, matchEventPublisherService.getMatchStream())
                .delayElements(Duration.ofSeconds(4));
    }

    @GetMapping(value = "/odds", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<MatchResponseDTO> streamOdds() {
        Flux<MatchResponseDTO> matches = matchService.findAll();
        return Flux.merge(matches, matchEventPublisherService.getOddsStream())
                .delayElements(Duration.ofSeconds(1));
    }

    @Override
    public Mono<ResponseEntity<MatchResponseDTO>> patchUpdateMatchScore(Long matchId, Mono<UpdateScoreRequestDTO> updateScoreRequestDTO, ServerWebExchange exchange) {
        return updateScoreRequestDTO
                .flatMap(dto -> matchService.updateMatchScore(
                        matchId,
                        dto.getHomeTeamScore(),
                        dto.getVisitingTeamScore()
                ))
                .doOnSuccess(matchEventPublisherService::publishOddsUpdate)
                .map(ResponseEntity::ok);
    }

    @GetMapping(value = "/scores", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<MatchResponseDTO> getStreamScores(){
        Flux<MatchResponseDTO> matches = matchService.findAllInProgress();
        return Flux.merge(matches, matchEventPublisherService.getOddsStream())
                .delayElements(Duration.ofSeconds(1));
    }
}