package br.com.hahn.toxicbet.application.controller;

import br.com.hahn.toxicbet.api.MatchApi;
import br.com.hahn.toxicbet.application.service.MatchEventPublisherService;
import br.com.hahn.toxicbet.application.service.MatchService;
import br.com.hahn.toxicbet.model.CloseMatchRequestDTO;
import br.com.hahn.toxicbet.model.MatchRequestDTO;
import br.com.hahn.toxicbet.model.MatchResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequiredArgsConstructor
@Slf4j
public class MatchController extends AbstractController implements MatchApi {

    private final MatchService matchService;
    private final MatchEventPublisherService matchEventPublisherService;

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
        return withLiveEvents(existingMatches, eventStream);
    }

    @GetMapping(value = "/match/find-open", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<MatchResponseDTO> streamOpenBettingMatches() {
        Flux<MatchResponseDTO> openMatches = matchService.findMatchesOpenToBets();
        Flux<MatchResponseDTO> openEventsOnly = getEventStream()
                .filter(matchService::isOpenForBettingNow);
        return withLiveEvents(openMatches, openEventsOnly);
    }

    @GetMapping(value = "/match/in-progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<MatchResponseDTO> streamInProgressMatches() {
        Flux<MatchResponseDTO> inProgressMatches = matchService.findInProgressMatches();
        Flux<MatchResponseDTO> inProgressEventsOnly = getEventStream()
                .filter(match -> MatchResponseDTO.ResultEnum.IN_PROGRESS.equals(match.getResult()));
        return withLiveEvents(inProgressMatches, inProgressEventsOnly);
    }

    @GetMapping(value = "/match/open/by-championship", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<MatchResponseDTO> streamOpenBettingMatchesByChampionship(@RequestParam Long championshipId) {
        Flux<MatchResponseDTO> openMatchesByChampionship = matchService.findOpenMatchesByChampionship(championshipId);
        Flux<MatchResponseDTO> openEventsOnly = getEventStream()
                .filter(m -> m.getChampionshipId() != null
                        && m.getChampionshipId().equals(championshipId)
                        && matchService.isOpenForBettingNow(m));
        return withLiveEvents(openMatchesByChampionship, openEventsOnly);
    }

    @GetMapping(value = "/match/by-championship", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<MatchResponseDTO> streamMatchesByChampionship(@RequestParam Long championshipId) {
        Flux<MatchResponseDTO> matchesByChampionship = matchService.findMatchByChampionship(championshipId);
        Flux<MatchResponseDTO> matchesEventsByChampionship = getEventStream()
                .filter(m -> m.getChampionshipId() != null && m.getChampionshipId().equals(championshipId));
        return withLiveEvents(matchesByChampionship, matchesEventsByChampionship);
    }

    @Override
    public Mono<ResponseEntity<Void>> closeMatchForBet(Long matchId, ServerWebExchange exchange) {
        return extractUserEmailFromToken(exchange)
                .flatMap(userEmail -> matchService.closeMatchForBet(matchId, userEmail))
                .then(Mono.just(ResponseEntity.status(HttpStatus.NO_CONTENT).build()));
    }

    @Override
    public Mono<ResponseEntity<Void>> patchCloseMatch(Flux<CloseMatchRequestDTO> closeMatchRequests, ServerWebExchange exchange) {
        return extractUserEmailFromToken(exchange)
                .flatMap(userEmail -> matchService.closeBatchMatches(closeMatchRequests, userEmail))
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

    private Flux<MatchResponseDTO> withLiveEvents(
            Flux<MatchResponseDTO> existingMatches,
            Flux<MatchResponseDTO> eventStream
    ) {
        return Flux.create(sink -> {
            Queue<MatchResponseDTO> eventsDuringSnapshot = new ConcurrentLinkedQueue<>();
            AtomicBoolean snapshotCompleted = new AtomicBoolean(false);

            Disposable liveSubscription = eventStream.subscribe(
                    event -> {
                        if (snapshotCompleted.get()) {
                            sink.next(event);
                        } else {
                            eventsDuringSnapshot.add(event);
                        }
                    },
                    sink::error
            );

            Disposable snapshotSubscription = existingMatches.subscribe(
                    sink::next,
                    sink::error,
                    () -> {
                        snapshotCompleted.set(true);
                        MatchResponseDTO event;
                        while (!sink.isCancelled() && (event = eventsDuringSnapshot.poll()) != null) {
                            sink.next(event);
                        }
                    }
            );

            sink.onDispose(() -> {
                snapshotSubscription.dispose();
                liveSubscription.dispose();
                eventsDuringSnapshot.clear();
            });
        }, FluxSink.OverflowStrategy.BUFFER);
    }
}
