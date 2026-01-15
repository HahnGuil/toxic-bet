package br.com.hahn.toxicbet.application.controller;

import br.com.hahn.toxicbet.api.UsersApi;
import br.com.hahn.toxicbet.application.service.UserService;
import br.com.hahn.toxicbet.infrastructure.service.JwtService;
import br.com.hahn.toxicbet.model.UserRequestDTO;
import br.com.hahn.toxicbet.model.UserResponseDTO;
import br.com.hahn.toxicbet.util.DateTimeConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class UserController extends AbstractController implements UsersApi {

    private final UserService userService;

    public UserController(JwtService jwtService, UserService userService) {
        super(jwtService);
        this.userService = userService;
    }

    public Mono<ResponseEntity<UserResponseDTO>> postRegisterUser(Mono<UserRequestDTO> userRequestDTO, ServerWebExchange exchange) {
        return userRequestDTO
                .flatMap(req ->
                        DateTimeConverter.formatInstantNowReactive()
                                .doOnNext(ts -> log.info("UserController: Starting user registration for {} at: {}", req.getEmail(), ts))
                                .then(updateOAuthUserApplicationWithLogging(req.getEmail()).then(userService.registerUser(req)))
                )
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }
}