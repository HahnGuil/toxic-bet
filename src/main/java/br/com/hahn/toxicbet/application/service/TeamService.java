package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.domain.model.Team;
import br.com.hahn.toxicbet.domain.repository.TeamRepository;
import br.com.hahn.toxicbet.util.DateTimeConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {

    private final TeamRepository repository;

    public Mono<Team> findById(Long id){
        log.info("TeamService: Find Team by id: {} at: {}", id, DateTimeConverter.formatInstantNow());
        return repository.findById(id);
    }

}
