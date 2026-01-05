package br.com.hahn.toxicbet.application.controller;

import br.com.hahn.toxicbet.api.BetApi;
import br.com.hahn.toxicbet.application.service.BetService;
import br.com.hahn.toxicbet.infrastructure.service.JwtService;
import br.com.hahn.toxicbet.model.BetRequestDTO;
import br.com.hahn.toxicbet.model.BetResponseDTO;
import br.com.hahn.toxicbet.util.DateTimeConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class BetController extends AbstractController implements BetApi {

    private final BetService betService;


    public BetController(JwtService jwtService, BetService betService) {
        super(jwtService);
        this.betService = betService;
    }

    /**
     * Handles the HTTP POST request to register a new bet.
     *
     * <p>
     * This method performs the following steps:
     * <ol>
     *     <li>Extracts the user ID from the JWT token in the {@link ServerWebExchange}.</li>
     *     <li>Logs the start of the bet creation process with the current timestamp.</li>
     *     <li>Delegates to the {@link BetService#placeBet(Mono, String)} method to process the bet.</li>
     *     <li>Logs the successful placement of the bet with the current timestamp.</li>
     *     <li>Wraps the resulting {@link BetResponseDTO} in a {@link ResponseEntity} with HTTP status 201 (Created).</li>
     * </ol>
     * </p>
     *
     * @author HahnGuil
     * @param betRequestDTO a {@link Mono} emitting the {@link BetRequestDTO} containing the bet details.
     * @param exchange the {@link ServerWebExchange} containing the HTTP request and response.
     * @return a {@link Mono} emitting a {@link ResponseEntity} containing the {@link BetResponseDTO} of the created bet.
     */
    @Override
    public Mono<ResponseEntity<BetResponseDTO>> postRegisterBet(Mono<BetRequestDTO> betRequestDTO, ServerWebExchange exchange) {
        return extractUserIdFromToken(exchange)
                .flatMap(userEmail ->
                        DateTimeConverter.formatInstantNowReactive()
                                .doOnNext(ts -> log.info("BetController: Starting create bet for user: {} at: {}", userEmail, ts))
                                .then(betService.placeBet(betRequestDTO, userEmail))
                                .doOnSuccess(dto -> log.info("BetController: Bet placed for user: {} at: {}", userEmail, DateTimeConverter.formatInstantNow()))
                                .map(dto -> ResponseEntity.status(HttpStatus.CREATED).body(dto))
                );
    }
}

