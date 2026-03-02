package br.com.hahn.toxicbet.application.controller;

import br.com.hahn.toxicbet.api.BettingPoolApi;
import br.com.hahn.toxicbet.application.service.BettingPoolService;
import br.com.hahn.toxicbet.domain.model.dto.UserDTO;
import br.com.hahn.toxicbet.infrastructure.service.JwtService;
import br.com.hahn.toxicbet.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/bettingPool")
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
    public Mono<ResponseEntity<SuccessResponseDTO>> postAddUserToBettingPool(String bettingPoolKey, ServerWebExchange exchange) {
        return null;
    }

    @Override
    public Mono<ResponseEntity<BetResponseDTO>> postBettingPool(Mono<BettingPoolRequestDTO> bettingPoolRequestDTO, ServerWebExchange exchange) {
        return null;
    }
}
