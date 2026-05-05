package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.model.MatchResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
@Slf4j
public class MatchEventPublisherService {

    private final Sinks.Many<MatchResponseDTO> sink;
    private final Object emitLock = new Object();

    public MatchEventPublisherService() {
        this.sink = Sinks.many().multicast().onBackpressureBuffer();
    }

    public void publishMatchUpdate(MatchResponseDTO match) {
        int subscribers = sink.currentSubscriberCount();

        Sinks.EmitResult result;
        synchronized (emitLock) {
            result = sink.tryEmitNext(match);
        }

        if (subscribers == 0) {
            log.error("MatchEventPublisherService: No subscribers for match event {}. emitResult={}",
                    match.getMatchId(), result);
        }

        if (result.isFailure()) {
            log.error("MatchEventPublisherService: Failed to emit match event {}. subscribers={}, result={}",
                    match.getMatchId(), subscribers, result);
        }
    }

    public void publishOddsUpdate(MatchResponseDTO match) {
        publishMatchUpdate(match);
    }

    public Flux<MatchResponseDTO> getAllEventsStream() {
        return sink.asFlux();
    }
}
