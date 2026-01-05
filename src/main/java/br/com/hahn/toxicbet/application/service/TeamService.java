package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.domain.model.Team;
import br.com.hahn.toxicbet.domain.repository.TeamRepository;
import br.com.hahn.toxicbet.util.DateTimeConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


/**
 * Service class responsible for handling operations related to the Team entity.
 *
 * @author HahnGuil
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {

    private final TeamRepository repository;

    /**
     * Finds a Team by its ID.
     *
     * @author HahnGuil
     * @param id The ID of the Team to be retrieved.
     * @return A {@link Mono} containing the Team if found, or empty if not found.
     */
    public Mono<Team> findById(Long id){
        log.info("TeamService: Find Team by id: {} at: {}", id, DateTimeConverter.formatInstantNow());
        return repository.findById(id);
    }

}