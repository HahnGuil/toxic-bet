package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.domain.model.Championship;
import br.com.hahn.toxicbet.domain.repository.ChampionshipRepository;
import br.com.hahn.toxicbet.util.DateTimeConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChampionshipService {

    private final ChampionshipRepository repository;

    public Mono<Championship> findById(Long id){
        log.info("ChampionshipService: Find Championship by id: {}, at: {}", id, DateTimeConverter.formatInstantNow());
        return repository.findById(id);
    }


}
