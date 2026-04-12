package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.application.mapper.TeamMapper;
import br.com.hahn.toxicbet.domain.exception.ConflictException;
import br.com.hahn.toxicbet.domain.model.Team;
import br.com.hahn.toxicbet.domain.model.enums.ErrorMessages;
import br.com.hahn.toxicbet.domain.repository.TeamRepository;
import br.com.hahn.toxicbet.model.TeamRequestDTO;
import br.com.hahn.toxicbet.model.TeamResponseDTO;
import br.com.hahn.toxicbet.util.DateTimeConverter;
import com.nimbusds.jose.crypto.impl.PRFParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {

    private final TeamRepository repository;
    private final TeamMapper mapper;
    private final UserService userService;

    public Mono<Team> findById(Long id){
        log.info("TeamService: Find Team by id: {} at: {}", id, DateTimeConverter.formatInstantNow());
        return repository.findById(id);
    }

    public Flux<TeamResponseDTO> createTeam(Flux<TeamRequestDTO> requestDTOFlux, String userEmail){
        return userService.isUserAdmin(userEmail)
                .thenMany(requestDTOFlux)
                .concatMap(this::validationAndSave);
    }

    private Mono<TeamResponseDTO> validationAndSave(TeamRequestDTO requestDTO){
        String teamName = requestDTO.getName() == null ? null : requestDTO.getName().trim();

        if (teamName == null || teamName.isBlank()) {
            return Mono.error(new IllegalArgumentException(ErrorMessages.NO_TEAM_NAME.getMessage()));
        }

        return repository.findTeamByName(teamName)
                .flatMap(exists -> Mono.<TeamResponseDTO>error(
                        new ConflictException(ErrorMessages.TEAM_ALREADY_EXISTS.getMessage())))
                .switchIfEmpty(Mono.defer(() -> {
                    var team = mapper.toEntity(requestDTO);
                    team.setName(teamName);
                    return repository.save(team).map(mapper::toDTO);
                }));
    }
}