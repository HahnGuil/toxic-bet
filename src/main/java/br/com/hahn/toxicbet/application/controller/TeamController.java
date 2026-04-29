package br.com.hahn.toxicbet.application.controller;

import br.com.hahn.toxicbet.api.TeamApi;
import br.com.hahn.toxicbet.application.service.TeamService;
import br.com.hahn.toxicbet.model.TeamRequestDTO;
import br.com.hahn.toxicbet.model.TeamResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class TeamController extends AbstractController implements TeamApi {

    private final TeamService teamService;

    @Override
    public Mono<ResponseEntity<Flux<TeamResponseDTO>>> postCreateTeams(Flux<TeamRequestDTO> teamRequestDTO, ServerWebExchange exchange) {
        return extractUserEmailFromToken(exchange)
                .map(userEmail ->
                        ResponseEntity.status(HttpStatus.CREATED).body(teamService.createTeam(teamRequestDTO, userEmail)));
    }
}
