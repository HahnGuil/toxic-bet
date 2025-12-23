package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.application.mapper.MatchMapper;
import br.com.hahn.toxicbet.domain.exception.InvalidMatchTimeException;
import br.com.hahn.toxicbet.domain.exception.TeamNotFoundException;
import br.com.hahn.toxicbet.domain.model.Match;
import br.com.hahn.toxicbet.domain.model.Team;
import br.com.hahn.toxicbet.domain.model.enums.ErrorMessages;
import br.com.hahn.toxicbet.domain.model.enums.Result;
import br.com.hahn.toxicbet.domain.repository.MatchRepository;
import br.com.hahn.toxicbet.model.MatchRequestDTO;
import br.com.hahn.toxicbet.model.MatchResponseDTO;
import br.com.hahn.toxicbet.util.DateTimeConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchService {

    private final TeamService teamService;
    private final MatchRepository repository;
    private final MatchMapper mapper;

    public Mono<MatchResponseDTO> createMatchDto(Mono<MatchRequestDTO> matchRequestDTOMono) {
        return matchRequestDTOMono
                .flatMap(this::isMatchTimeValid)
                .flatMap(dto -> {
                    Mono<Team> home = teamService.findById(dto.getHomeTeamId())
                            .switchIfEmpty(Mono.error(new TeamNotFoundException(ErrorMessages.HOME_TEAM_NOT_FOUND.getMessage())));

                    Mono<Team> visiting = teamService.findById(dto.getVisitingTeamId())
                            .switchIfEmpty(Mono.error(new TeamNotFoundException(ErrorMessages.VISITING_TEAM_NOT_FOUND.getMessage())));

                    return Mono.zip(home, visiting)
                            .flatMap(tuple -> {
                                var entity = mapper.toEntity(dto);
                                entity.setResult(Result.NOT_STARTED);
                                return repository.save(entity)
                                        .map(savedMatch -> mapper.toDto(savedMatch, tuple.getT1().getName(), tuple.getT2().getName()));
                            });
                });
    }

    public Flux<MatchResponseDTO> findAll(){
        log.info("MatchService: Find all matches at: {}", DateTimeConverter.formatInstantNow());
        return repository.findAll()
                .flatMap(match -> {
                    Mono<Team> home = teamService.findById(match.getHomeTeamId());
                    Mono<Team> visiting = teamService.findById(match.getVisitingTeamId());

                    return Mono.zip(home, visiting)
                            .map(tuple -> mapper.toDto(match, tuple.getT1().getName(), tuple.getT2().getName()));
                });
    }

    private Mono<MatchRequestDTO> isMatchTimeValid(MatchRequestDTO dto) {
        log.info("MatchService: Validating match time at: {}", DateTimeConverter.formatInstantNow());
        var matchTime = DateTimeConverter.parseToLocalDateTime(dto.getMatchTime());
        assert matchTime != null;
        if (matchTime.isBefore(LocalDateTime.now())) {
            return Mono.error(new InvalidMatchTimeException("Match time has already passed"));
        }
        return Mono.just(dto);
    }
}


