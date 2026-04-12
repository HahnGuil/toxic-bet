package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.application.mapper.ChampionshipMapper;
import br.com.hahn.toxicbet.domain.exception.NotFoundException;
import br.com.hahn.toxicbet.domain.model.Championship;
import br.com.hahn.toxicbet.domain.model.Team;
import br.com.hahn.toxicbet.domain.model.enums.ErrorMessages;
import br.com.hahn.toxicbet.domain.repository.ChampionshipRepository;
import br.com.hahn.toxicbet.model.ChampionshipDTO;
import br.com.hahn.toxicbet.model.ChampionshipRequestDTO;
import br.com.hahn.toxicbet.model.SuccessResponseDTO;
import br.com.hahn.toxicbet.util.DateTimeConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.util.Tuple;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChampionshipService {

    private final ChampionshipRepository repository;
    private final UserService userService;
    private final ChampionshipMapper mapper;
    private final TeamService teamService;

    public Mono<ChampionshipDTO> createChampionship(Mono<ChampionshipRequestDTO> requestDTOMono, String userEmail) {
        return userService.isUserAdmin(userEmail)
                .then(requestDTOMono)
                .map(mapper::toEntity)
                .flatMap(repository::save)
                .map(mapper::toDTO);
    }

    public Mono<Championship> findById(Long id){
        log.info("ChampionshipService: Find Championship by id: {}, at: {}", id, DateTimeConverter.formatInstantNow());
        return repository.findById(id);
    }

    public Flux<ChampionshipDTO> findAll(){
        return repository.findAll()
                .map(mapper::toDTO);
    }

    public Flux<Team> findTeamsByChampionship(Long championshipId) {
        return findById(championshipId)
                .switchIfEmpty(Mono.error(new NotFoundException(ErrorMessages.CHAMPIONSHIP_NOT_FOUND.getMessage())))
                .flatMapMany(championship -> {
                    List<String> teamIds = championship.getTeams();
                    if (teamIds == null || teamIds.isEmpty()) {
                        return Flux.empty();
                    }

                    return Flux.fromIterable(teamIds)
                            .map(Long::valueOf)
                            .concatMap(teamService::findById)
                            .switchIfEmpty(Mono.error(new NotFoundException(ErrorMessages.TEAM_NOT_FOUND.getMessage())));
                });
    }


    public Mono<SuccessResponseDTO> addTeamToChampionship(String userEmail, Long championshipId, Long teamId){
        return userService.isUserAdmin(userEmail)
                .then(validateChampionshipAndTeam(championshipId, teamId))
                .flatMap(tuple -> {
                    var championship = tuple.getT1();
                    var teamIdAsString = String.valueOf(tuple.getT2().getId());

                    List<String> teams = championship.getTeams() == null ? new ArrayList<>() : new ArrayList<>(championship.getTeams());
                    if(!teams.contains(teamIdAsString)){
                        teams.add(teamIdAsString);
                        championship.setTeams(teams);
                    }

                    return repository.save(championship)
                            .thenReturn(new SuccessResponseDTO().message("Team added successfully to championship: " + championship.getName()));
                });
    }

    private Mono<Tuple2<Championship, Team>> validateChampionshipAndTeam(Long championshipId, Long teamId){
        return Mono.zip(
                findById(championshipId), teamService.findById(teamId)
        );
    }

}