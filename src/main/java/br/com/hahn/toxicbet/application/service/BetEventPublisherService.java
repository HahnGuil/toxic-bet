package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.model.BetResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.UUID;

@Service
@Slf4j
public class BetEventPublisherService {

    public record UserBetEvent(UUID userId, BetResponseDTO bet) {}

    private final Sinks.Many<UserBetEvent> sink;
    private final Object emitLock = new Object();

    public BetEventPublisherService() {
        this.sink = Sinks.many().multicast().onBackpressureBuffer();
    }

    public void publishBetPlaced(UUID userId, BetResponseDTO bet) {
        Sinks.EmitResult result;
        synchronized (emitLock) {
            result = sink.tryEmitNext(new UserBetEvent(userId, bet));
        }
        if (result.isFailure()) {
            log.warn("BetEventPublisherService: Failed to emit bet event for user {}: {}", userId, result);
        }
    }

    public Flux<BetResponseDTO> streamBetsForUser(UUID userId) {
        return sink.asFlux()
                .filter(event -> userId.equals(event.userId()))
                .map(UserBetEvent::bet);
    }
}
