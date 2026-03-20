package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.model.MatchResponseDTO;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class MatchEventPublisherService {

    private final Sinks.Many<MatchResponseDTO> matchSink;
    private final Sinks.Many<MatchResponseDTO> oddsSink;

    public MatchEventPublisherService() {
        this.matchSink = Sinks.many().multicast().onBackpressureBuffer();
        this.oddsSink = Sinks.many().multicast().onBackpressureBuffer();
    }

    public void publishOddsUpdate(MatchResponseDTO matchResponseDTO){
        oddsSink.tryEmitNext(matchResponseDTO);
    }

    public Flux<MatchResponseDTO> getAllEventsStream() {
        return Flux.merge(matchSink.asFlux(), oddsSink.asFlux());
    }
}
