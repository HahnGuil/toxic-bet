package br.com.hahn.toxicbet.application.controller;

import br.com.hahn.toxicbet.api.UsersApi;
import br.com.hahn.toxicbet.application.service.UserService;
import br.com.hahn.toxicbet.domain.model.dto.UserDTO;
import br.com.hahn.toxicbet.infrastructure.service.JwtService;
import br.com.hahn.toxicbet.model.UserRequestDTO;
import br.com.hahn.toxicbet.model.UserResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@Slf4j
public class UserController extends AbstractController implements UsersApi {

    private final UserService userService;

    public UserController(JwtService jwtService, UserService userService) {
        super(jwtService);
        this.userService = userService;
    }

    public Mono<ResponseEntity<Void>> postRegisterUser(Mono<UserRequestDTO> userRequestDTO, ServerWebExchange exchange) {
        return userRequestDTO.flatMap(userService::registerUser)
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