package br.com.hahn.toxicbet.application.controller;

import br.com.hahn.toxicbet.api.BettingPoolApi;
import br.com.hahn.toxicbet.application.service.BettingPoolService;
import br.com.hahn.toxicbet.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class BettingPoolController extends AbstractController implements BettingPoolApi {

    private final BettingPoolService bettingPoolService;

    @Override
    public Mono<ResponseEntity<BettingPoolResponseDTO>> getBettingPoolByUniqueCode(String bettingPoolKey, ServerWebExchange exchange) {
        return bettingPoolService.getBettingPoolByKey(bettingPoolKey)
                .map(bettingPoolResponseDTO -> ResponseEntity.status(HttpStatus.OK).body(bettingPoolResponseDTO));
    }

    @Override
    public Mono<ResponseEntity<Flux<BettingPoolUsersResponseDTO>>> getUsersByBettingPool(ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.status(HttpStatus.OK)
                .body(bettingPoolService.getBettingPoolUsers(extractUserEmailFromToken(exchange))));
    }

    @Override
    public Mono<ResponseEntity<SuccessResponseDTO>> patchAddUserToBettingPool(String bettingPoolKey, ServerWebExchange exchange) {
        return bettingPoolService.addUserToBettingPool(bettingPoolKey, extractUserEmailFromToken(exchange))
                .map(successResponseDTO -> ResponseEntity.status(HttpStatus.OK).body(successResponseDTO));
    }

    @Override
    public Mono<ResponseEntity<BettingPoolResponseDTO>> postBettingPool(Mono<BettingPoolRequestDTO> bettingPoolRequestDTO, ServerWebExchange exchange) {
        return bettingPoolService.createBettingPool(bettingPoolRequestDTO, extractUserEmailFromToken(exchange))
                .map(bettingPoolResponseDTO -> ResponseEntity.status(HttpStatus.CREATED).body(bettingPoolResponseDTO));
    }
}