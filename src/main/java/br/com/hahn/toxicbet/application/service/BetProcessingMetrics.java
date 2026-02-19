package br.com.hahn.toxicbet.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple metrics collector for bet processing monitoring.
 * Tracks queue sizes and processing counts per match.
 * Note: For production, integrate with Micrometer/Prometheus for full observability.
 */
@Component
@Slf4j
public class BetProcessingMetrics {

    private final ConcurrentMap<Long, AtomicLong> queueSizes = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, AtomicLong> processedCount = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, AtomicLong> failedCount = new ConcurrentHashMap<>();

    /**
     * Record a bet enqueued for processing
     */
    public void recordBetEnqueued(Long matchId) {
        queueSizes.computeIfAbsent(matchId, id -> new AtomicLong(0)).incrementAndGet();

        log.debug("BetProcessingMetrics: Bet enqueued for match {}. Current queue size: {}",
                matchId, queueSizes.get(matchId).get());
    }

    /**
     * Record a bet processing started
     */
    public ProcessingSample recordProcessingStarted(Long matchId) {
        AtomicLong queueSize = queueSizes.get(matchId);
        if (queueSize != null) {
            queueSize.decrementAndGet();
        }

        return new ProcessingSample(System.nanoTime());
    }

    /**
     * Record a bet processing completed successfully
     */
    public void recordProcessingCompleted(Long matchId, ProcessingSample sample) {
        processedCount.computeIfAbsent(matchId, id -> new AtomicLong(0)).incrementAndGet();

        long durationMs = (System.nanoTime() - sample.startTime()) / 1_000_000;

        log.debug("BetProcessingMetrics: Bet processing completed for match {} in {}ms. Total processed: {}",
                matchId, durationMs, processedCount.get(matchId).get());
    }

    /**
     * Record a bet processing failed
     */
    public void recordProcessingFailed(Long matchId, Throwable error) {
        failedCount.computeIfAbsent(matchId, id -> new AtomicLong(0)).incrementAndGet();

        log.warn("BetProcessingMetrics: Bet processing failed for match {}. Total failed: {}. Error: {}",
                matchId, failedCount.get(matchId).get(), error.getMessage());
    }

    /**
     * Get current queue size for a match
     */
    public long getQueueSize(Long matchId) {
        AtomicLong size = queueSizes.get(matchId);
        return size != null ? size.get() : 0;
    }

    /**
     * Get total number of matches being processed
     */
    public int getActiveMatchesCount() {
        return queueSizes.size();
    }

    /**
     * Get processing statistics for a match
     */
    public String getMatchStatistics(Long matchId) {
        long processed = processedCount.getOrDefault(matchId, new AtomicLong(0)).get();
        long failed = failedCount.getOrDefault(matchId, new AtomicLong(0)).get();
        long queueSize = getQueueSize(matchId);

        return String.format("Match %d: Processed=%d, Failed=%d, QueueSize=%d",
                matchId, processed, failed, queueSize);
    }

    /**
     * Clean up metrics for finished matches
     */
    public void cleanupMatchMetrics(Long matchId) {
        queueSizes.remove(matchId);
        processedCount.remove(matchId);
        failedCount.remove(matchId);

        log.info("BetProcessingMetrics: Cleaned up metrics for match {}", matchId);
    }

    /**
     * Simple processing sample record
     */
    public record ProcessingSample(long startTime) {}
}

