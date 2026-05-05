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

    public MatchEventPublisherService() {
        this.sink = Sinks.many().multicast().onBackpressureBuffer();
    }

    public void publishMatchUpdate(MatchResponseDTO match) {

        Sinks.EmitResult result = sink.tryEmitNext(match);

        if (result.isFailure()) {
            log.warn("Falha ao emitir evento de partida: {}", result);
        }
    }

    public void publishOddsUpdate(MatchResponseDTO match) {
        publishMatchUpdate(match);
    }

    public Flux<MatchResponseDTO> getAllEventsStream() {
        return sink.asFlux();
    }
}
