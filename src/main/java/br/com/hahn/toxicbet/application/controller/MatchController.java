package br.com.hahn.toxicbet.application.controller;

import br.com.hahn.toxicbet.api.MatchApi;
import br.com.hahn.toxicbet.application.mapper.MatchMapper;
import br.com.hahn.toxicbet.application.service.MatchService;
import br.com.hahn.toxicbet.domain.model.Match;
import br.com.hahn.toxicbet.model.MatchRequestDTO;
import br.com.hahn.toxicbet.model.MatchResponseDTO;
import br.com.hahn.toxicbet.util.DateTimeConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;

@RestController
@RequestMapping("/match")
@Slf4j
public class MatchController implements MatchApi {

    /**
     * TODO
     * 1 - MUDAR O DTO DE RESPOSTA DA CRIAÇÃO PARA SUCESSO
     * 2 - MUDAR O DTO DE RESPOSTA DO GET ALL PARA UM QUE TRAGA O NOME DOS TIMES E NÃO OS ID
     * 3 - IMPLEMENTAR NO TEAM SERVER UM FLUXO NÃO BLOQUEANTE PARA PEGAR OS NOMES
     */

    private final Sinks.Many<Match> eventoSink;
    private final MatchService matchService;
    private final MatchMapper mapper;

    public MatchController(MatchService matchService, MatchMapper mapper) {
        this.matchService = matchService;
        this.mapper = mapper;
        this.eventoSink = Sinks.many().multicast().onBackpressureBuffer();
    }

    @Override
    public Mono<ResponseEntity<MatchResponseDTO>> postCreateMatch(Mono<MatchRequestDTO> matchRequestDTO, ServerWebExchange exchange) {
        return DateTimeConverter.formatInstantNowReactive()
                .doOnNext(ts -> log.info("MatchController: Starting creating match at: {}", ts))
                .then(matchService.createMatchEntity(matchRequestDTO))
                .doOnSuccess(eventoSink::tryEmitNext)
                .map(match -> ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDto(match)));
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Match> streamMatches() {
        Flux<Match> matches = matchService.findAll();
        return Flux.merge(matches, eventoSink.asFlux())
                .delayElements(Duration.ofSeconds(4));
    }
}