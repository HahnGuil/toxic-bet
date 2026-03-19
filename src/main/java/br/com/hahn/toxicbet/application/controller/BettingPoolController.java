package br.com.hahn.toxicbet.application.controller;

import br.com.hahn.toxicbet.api.BettingPoolApi;
import br.com.hahn.toxicbet.application.service.BettingPoolService;
import br.com.hahn.toxicbet.infrastructure.service.JwtService;
import br.com.hahn.toxicbet.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class BettingPoolController extends AbstractController implements BettingPoolApi {

    private final BettingPoolService bettingPoolService;

    public BettingPoolController(JwtService jwtService, BettingPoolService bettingPoolService) {
        super(jwtService);
        this.bettingPoolService = bettingPoolService;
    }

    @Override
    public Mono<ResponseEntity<BettingPoolResponseDTO>> getBettingPoolByUniqueCode(String bettingPoolKey, ServerWebExchange exchange) {
        return bettingPoolService.getBettingPoolByKey(bettingPoolKey)
                .map(bettingPoolResponseDTO -> ResponseEntity.status(HttpStatus.OK).body(bettingPoolResponseDTO));
    }

    @Override
    public Mono<ResponseEntity<BettingPoolUsersResponseDTO>> getUsersByBettingPool(String bettingPoolKey, ServerWebExchange exchange) {
        return bettingPoolService.getBettingPoolUsers(bettingPoolKey)
                .map(bettingPoolUsersResponseDTO -> ResponseEntity.status(HttpStatus.OK).body(bettingPoolUsersResponseDTO));
    }

    @Override
    public Mono<ResponseEntity<SuccessResponseDTO>> patchAddUserToBettingPool(String bettingPoolKey, ServerWebExchange exchange) {
        return bettingPoolService.addUserToBettingPool(bettingPoolKey, extractUserIdFromToken(exchange))
                .map(successResponseDTO -> ResponseEntity.status(HttpStatus.OK).body(successResponseDTO));
    }

    @Override
    public Mono<ResponseEntity<BettingPoolResponseDTO>> postBettingPool(Mono<BettingPoolRequestDTO> bettingPoolRequestDTO, ServerWebExchange exchange) {
        return bettingPoolService.createBettingPool(bettingPoolRequestDTO, extractUserEmailFromToken(exchange))
                .map(bettingPoolResponseDTO -> ResponseEntity.status(HttpStatus.CREATED).body(bettingPoolResponseDTO));
    }
}
