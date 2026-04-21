package br.com.hahn.toxicbet.application.controller;

import br.com.hahn.toxicbet.api.ChampionshipApi;
import br.com.hahn.toxicbet.application.service.ChampionshipService;
import br.com.hahn.toxicbet.infrastructure.service.JwtService;
import br.com.hahn.toxicbet.model.ChampionshipDTO;
import br.com.hahn.toxicbet.model.ChampionshipRequestDTO;
import br.com.hahn.toxicbet.model.SuccessResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class ChampionshipController extends AbstractController implements ChampionshipApi {

    private final ChampionshipService championshipService;

    public ChampionshipController(JwtService jwtService, ChampionshipService championshipService) {
        super(jwtService);
        this.championshipService = championshipService;
    }

    @Override
    public Mono<ResponseEntity<ChampionshipDTO>> championshipPost(Mono<ChampionshipRequestDTO> championshipRequestDTO, ServerWebExchange exchange) {
        return extractUserEmailFromToken(exchange)
                .flatMap(userEmail -> championshipService.createChampionship(championshipRequestDTO, userEmail))
                .map(dto -> ResponseEntity.status(HttpStatus.CREATED).body(dto));
    }

    @Override
    public Mono<ResponseEntity<Flux<ChampionshipDTO>>> getFindAll(ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.status(HttpStatus.OK).body(championshipService.findAll()));
    }

    @Override
    public Mono<ResponseEntity<Flux<ChampionshipDTO>>> getTeamByChampioship(Long championshipId, ServerWebExchange exchange) {
        Flux<ChampionshipDTO> teams = championshipService.findTeamsByChampionship(championshipId)
                .map(team -> new ChampionshipDTO()
                        .idChampionship(team.getId())
                        .name(team.getName()));
        return Mono.just(ResponseEntity.status(HttpStatus.OK).body(teams));
    }
    
    @Override
    public Mono<ResponseEntity<SuccessResponseDTO>> patchAddTeamToChampionship(Long championshipId, Long teamId, ServerWebExchange exchange) {
        return extractUserEmailFromToken(exchange)
                .flatMap(userEmail -> championshipService.addTeamToChampionship(userEmail, championshipId, teamId))
                .map(response -> ResponseEntity.status(HttpStatus.OK).body(response));
    }
}