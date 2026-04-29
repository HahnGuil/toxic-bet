package br.com.hahn.toxicbet.application.controller;

import br.com.hahn.toxicbet.api.UsersApi;
import br.com.hahn.toxicbet.application.service.UserService;
import br.com.hahn.toxicbet.domain.model.dto.UserDTO;
import br.com.hahn.toxicbet.model.UserResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@Slf4j
@RequiredArgsConstructor
public class UserController extends AbstractController implements UsersApi {

    private final UserService userService;

    public Mono<ResponseEntity<Void>> postRegisterUser(Mono<String> userName, ServerWebExchange exchange) {
        String authorizationHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        return Mono.zip(userName, extractUserEmailFromToken(exchange))
                .flatMap(tuple -> userService.registerUser(tuple.getT1(), tuple.getT2(), authorizationHeader))
                .then(Mono.just(ResponseEntity.status(HttpStatus.CREATED).build()));
    }

    @Override
    public Mono<ResponseEntity<UserResponseDTO>> getUser(UUID userId, String userEmail, ServerWebExchange exchange) {
        var userDTO = new UserDTO(userId, userEmail);
        return userService.getUser(userDTO)
                .map(userResponseDTO -> ResponseEntity.status(HttpStatus.OK).body(userResponseDTO));
    }

    @Override
    public Mono<ResponseEntity<Boolean>> existsByEmail(String userEmail, ServerWebExchange exchange) {
        Mono<Boolean> response = userService.existsByEmail(userEmail);
        return response.map(exists -> ResponseEntity.status(HttpStatus.OK).body(exists));
    }
}