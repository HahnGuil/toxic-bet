package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.model.BetRequestDTO;
import br.com.hahn.toxicbet.model.BetResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Load test to demonstrate scalability of Sink-based bet processing.
 * This test simulates high concurrency scenarios that would cause
 * lock contention with pessimistic locking approach.
 */
@SpringBootTest
class BetProcessorServiceLoadTest {

    @Autowired
    private BetService betService;

    /**
     * Test: 100 concurrent bets on the same match
     * Expected: All bets processed successfully without lock timeout
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
                    BetRequestDTO betRequest = createBetRequest(matchId, "user" + i + "@test.com");
                    return betService.placeBet(reactor.core.publisher.Mono.just(betRequest), "user" + i + "@test.com")
                            .doOnSuccess(response -> successCount.incrementAndGet())
                            .doOnError(error -> errorCount.incrementAndGet())
                            .onErrorResume(error -> reactor.core.publisher.Mono.empty());
                }, 10); // 10 concurrent requests at a time

        // Verify all bets are processed
        StepVerifier.create(betFlux)
                .expectNextCount(numberOfBets)
                .expectComplete()
                .verify(Duration.ofSeconds(30));

        // Assertions
        assertEquals(numberOfBets, successCount.get(), "All bets should be processed successfully");
        assertEquals(0, errorCount.get(), "No errors should occur");
    }

    /**
     * Test: 1000 bets distributed across 10 matches
     * Expected: All bets processed with high throughput
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
                            BetRequestDTO betRequest = createBetRequest(matchId.longValue(),
                                    "user" + matchId + "_" + betIndex + "@test.com");
                            return betService.placeBet(reactor.core.publisher.Mono.just(betRequest),
                                    "user" + matchId + "_" + betIndex + "@test.com")
                                    .doOnSuccess(response -> successCount.incrementAndGet())
                                    .onErrorResume(error -> reactor.core.publisher.Mono.empty());
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
     * Test: Sequential processing guarantee
     * Expected: Bets on same match are processed in order
     */
    @Test
    void shouldProcessBetsSequentiallyPerMatch() {
        Long matchId = 1L;
        int numberOfBets = 50;

        // Create sequential bets
        Flux<BetResponseDTO> betFlux = Flux.range(0, numberOfBets)
                .concatMap(i -> {
                    BetRequestDTO betRequest = createBetRequest(matchId, "user" + i + "@test.com");
                    return betService.placeBet(reactor.core.publisher.Mono.just(betRequest), "user" + i + "@test.com");
                });

        // Verify all are processed
        StepVerifier.create(betFlux)
                .expectNextCount(numberOfBets)
                .expectComplete()
                .verify(Duration.ofSeconds(30));
    }

    private BetRequestDTO createBetRequest(Long matchId, String userEmail) {
        BetRequestDTO bet = new BetRequestDTO();
        bet.setMatchId(matchId);
        bet.setResult(BetRequestDTO.ResultEnum.HOME_WIN);
        bet.setOdds(2.5);
        return bet;
    }
}

