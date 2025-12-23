package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.application.mapper.MatchMapper;
import br.com.hahn.toxicbet.domain.exception.InvalidMatchTimeException;
import br.com.hahn.toxicbet.domain.exception.TeamNotFoundException;
import br.com.hahn.toxicbet.domain.model.Match;
import br.com.hahn.toxicbet.domain.model.Team;
import br.com.hahn.toxicbet.domain.model.enums.ErrorMessages;
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

    public Mono<Match> createMatchEntity(Mono<MatchRequestDTO> matchRequestDTOMono) {
        log.info("MatchService: Checking if the match creation date is valid");

        return matchRequestDTOMono
                .flatMap(this::isMatchTimeValid)
                .flatMap(dto -> {
                    Mono<Team> home = teamService.findById(dto.getHomeTeamId())
                            .switchIfEmpty(Mono.defer(() ->
                                    DateTimeConverter.formatInstantNowReactive()
                                            .doOnNext(ts -> log.error("MatchService: Home team not found: {} at: {}", dto.getHomeTeamId(), ts))
                                            .then(Mono.error(new TeamNotFoundException(ErrorMessages.HOME_TEAM_NOT_FOUND.getMessage())))));

                    Mono<Team> visiting = teamService.findById(dto.getVisitingTeamId())
                            .switchIfEmpty(Mono.defer(() ->
                                    DateTimeConverter.formatInstantNowReactive()
                                            .doOnNext(ts -> log.error("MatchService: Visiting team not found: {} at: {}", dto.getVisitingTeamId(), ts))
                                            .then(Mono.error(new TeamNotFoundException(ErrorMessages.VISITING_TEAM_NOT_FOUND.getMessage())))));

                    log.info("MatchService: Save match at database at: {}", DateTimeConverter.formatInstantNow());
                    return Mono.zip(home, visiting)
                            .flatMap(tuple -> {
                                var entity = mapper.toEntity(dto);
                                return repository.save(entity);
                            });
                });
    }

    public Flux<Match> findAll(){
        log.info("MatchService: Find all matches at: {}", DateTimeConverter.formatInstantNow());
        return repository.findAll();
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


