package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.model.BetRequestDTO;
import br.com.hahn.toxicbet.model.BetResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Load test to demonstrate scalability of Sink-based bet processing.
 * This test simulates high concurrency scenarios that would cause
 * lock contention with pessimistic locking approach.
 */
@ExtendWith(MockitoExtension.class)
class BetProcessorServiceLoadTest {

    @Mock
    private BetExecutorService betExecutorService;

    @Mock
    private BetProcessingMetrics metrics;

    private BetProcessorService betProcessorService;

    @BeforeEach
    void setup() {
        betProcessorService = new BetProcessorService(betExecutorService, metrics);
        doNothing().when(metrics).recordBetEnqueued(any(Long.class));

        lenient().when(betExecutorService.processBet(any(BetProcessorService.BetRequest.class)))
                .thenAnswer(invocation -> {
                    BetProcessorService.BetRequest request = invocation.getArgument(0);
                    request.responseSink().success(toResponse(request.betRequestDTO()));
                    return Mono.empty();
                });
    }

    /**
     * Test: 100 concurrent bets on the same match.
     * Expected: All bets processed successfully.
     */
    @Test
    void shouldHandleHighConcurrencyOnSingleMatch() {
        Long matchId = 1L;
        int numberOfBets = 100;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // Create 100 concurrent bets on the same match
        Flux<BetResponseDTO> betFlux = Flux.range(0, numberOfBets)
                .flatMap(i -> {
                    BetRequestDTO betRequest = createBetRequest(matchId);
                    return betProcessorService.enqueueBet(betRequest, "user" + i + "@test.com")
                            .doOnSuccess(response -> successCount.incrementAndGet())
                            .doOnError(error -> errorCount.incrementAndGet())
                            .onErrorResume(error -> Mono.empty());
                }, 10); // 10 concurrent requests at a time

        // Verify all bets are processed
        StepVerifier.create(betFlux)
                .expectNextCount(numberOfBets)
                .expectComplete()
                .verify(Duration.ofSeconds(30));

        // Assertions
        assertEquals(numberOfBets, successCount.get(), "All bets should be processed successfully");
        assertEquals(0, errorCount.get(), "No errors should occur");
        verify(metrics, times(numberOfBets)).recordBetEnqueued(matchId);
    }

    /**
     * Test: 1000 bets distributed across 10 matches.
     * Expected: All bets processed.
     */
    @Test
    void shouldHandleHighThroughputAcrossMultipleMatches() {
        int numberOfMatches = 10;
        int betsPerMatch = 100;
        AtomicInteger successCount = new AtomicInteger(0);

        // Create 100 bets for each of 10 matches (1000 total)
        Flux<BetResponseDTO> betFlux = Flux.range(1, numberOfMatches)
                .flatMap(matchId ->
                    Flux.range(0, betsPerMatch)
                        .flatMap(betIndex -> {
                            BetRequestDTO betRequest = createBetRequest(matchId.longValue());
                            return betProcessorService.enqueueBet(betRequest,
                                    "user" + matchId + "_" + betIndex + "@test.com")
                                    .doOnSuccess(response -> successCount.incrementAndGet())
                                    .onErrorResume(error -> Mono.empty());
                        }, 20) // High concurrency
                );

        // Verify all bets are processed quickly
        StepVerifier.create(betFlux)
                .expectNextCount(numberOfMatches * betsPerMatch)
                .expectComplete()
                .verify(Duration.ofSeconds(60));

        assertEquals(numberOfMatches * betsPerMatch, successCount.get(),
                "All bets should be processed successfully");
    }

    /**
     * Test: Sequential processing guarantee.
     * Expected: No concurrent execution for the same match (max in-flight = 1).
     */
    @Test
    void shouldProcessBetsSequentiallyPerMatch() {
        Long matchId = 1L;
        int numberOfBets = 50;
        AtomicInteger inFlight = new AtomicInteger(0);
        AtomicInteger maxInFlight = new AtomicInteger(0);

        when(betExecutorService.processBet(any(BetProcessorService.BetRequest.class)))
                .thenAnswer(invocation -> {
                    BetProcessorService.BetRequest request = invocation.getArgument(0);
                    return Mono.delay(Duration.ofMillis(5))
                            .doOnNext(ignore -> {
                                int current = inFlight.incrementAndGet();
                                maxInFlight.accumulateAndGet(current, Math::max);
                                inFlight.decrementAndGet();
                                request.responseSink().success(toResponse(request.betRequestDTO()));
                            })
                            .then();
                });

        // Create concurrent bet submissions for the same match.
        Flux<BetResponseDTO> betFlux = Flux.range(0, numberOfBets)
                .flatMap(i -> {
                    BetRequestDTO betRequest = createBetRequest(matchId);
                    return betProcessorService.enqueueBet(betRequest, "user" + i + "@test.com");
                });

        // Verify all are processed
        StepVerifier.create(betFlux)
                .expectNextCount(numberOfBets)
                .expectComplete()
                .verify(Duration.ofSeconds(30));

        assertEquals(1, maxInFlight.get(), "Bets for same match must be processed sequentially");
    }

    private BetRequestDTO createBetRequest(Long matchId) {
        BetRequestDTO bet = new BetRequestDTO();
        bet.setMatchId(matchId);
        bet.setResult(BetRequestDTO.ResultEnum.HOME_WIN);
        bet.setOdds(2.5);
        return bet;
    }

    private BetResponseDTO toResponse(BetRequestDTO betRequestDTO) {
        BetResponseDTO response = new BetResponseDTO();
        response.setMatchId(betRequestDTO.getMatchId());
        response.setResult(betRequestDTO.getResult().name());
        response.setOdds(betRequestDTO.getOdds());
        return response;
    }
}

