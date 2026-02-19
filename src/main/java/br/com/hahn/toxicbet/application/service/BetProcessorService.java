package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.model.BetRequestDTO;
import br.com.hahn.toxicbet.model.BetResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service responsible for processing bets sequentially per match.
 * Uses a Sink per match with concatMap to ensure sequential processing
 * and avoid race conditions without database locking.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BetProcessorService {

    private final Map<Long, Sinks.Many<BetRequest>> matchSinks = new ConcurrentHashMap<>();
    private final BetExecutorService betExecutorService;
    private final BetProcessingMetrics metrics;

    /**
     * Enqueues a bet for processing. Bets for the same match are processed sequentially.
     *
     * @param betRequestDTO The bet request
     * @param userEmail The user email
     * @return Mono with the bet response
     */
    public Mono<BetResponseDTO> enqueueBet(BetRequestDTO betRequestDTO, String userEmail) {
        Long matchId = betRequestDTO.getMatchId();

        log.debug("BetProcessorService: Enqueuing bet for match {} by user {}", matchId, userEmail);
        metrics.recordBetEnqueued(matchId);

        return Mono.create(sink -> {
            BetRequest request = new BetRequest(betRequestDTO, userEmail, sink);

            Sinks.Many<BetRequest> matchSink = matchSinks.computeIfAbsent(matchId, id -> {
                log.info("BetProcessorService: Creating new Sink for match {}", id);
                Sinks.Many<BetRequest> newSink = Sinks.many().unicast().onBackpressureBuffer();

                // Setup sequential processing with concatMap
                newSink.asFlux()
                        .concatMap(betExecutorService::processBet)
                        .doOnError(error -> log.error("BetProcessorService: Error processing bet for match {}", id, error))
                        .onErrorResume(error -> {
                            log.warn("BetProcessorService: Recovered from error in flux for match {}, continuing", id);
                            return Mono.empty();
                        })
                        .subscribe(
                                unused -> log.trace("BetProcessorService: Bet processed for match {}", id),
                                error -> log.error("BetProcessorService: Fatal error in subscription for match {}", id, error),
                                () -> log.info("BetProcessorService: Flux completed for match {}", id)
                        );

                return newSink;
            });

            Sinks.EmitResult result = matchSink.tryEmitNext(request);

            if (result.isFailure()) {
                log.error("BetProcessorService: Failed to enqueue bet for match {}: {}", matchId, result);
                sink.error(new RuntimeException("Failed to enqueue bet: " + result));
            }
        });
    }

    /**
     * Internal class to hold bet request data
     */
    public record BetRequest(
            BetRequestDTO betRequestDTO,
            String userEmail,
            reactor.core.publisher.MonoSink<BetResponseDTO> responseSink
    ) {}
}

