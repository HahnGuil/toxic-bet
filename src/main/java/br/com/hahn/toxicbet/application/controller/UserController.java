package br.com.hahn.toxicbet.application.controller;

import br.com.hahn.toxicbet.api.UsersApi;
import br.com.hahn.toxicbet.application.service.UserService;
import br.com.hahn.toxicbet.model.UserRequestDTO;
import br.com.hahn.toxicbet.model.UserResponseDTO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/users")
@AllArgsConstructor
@Slf4j
public class UserController implements UsersApi {

    private final UserService userService;

    @Override
    public Mono<ResponseEntity<UserResponseDTO>> postRegisterUser(Mono<UserRequestDTO> userRequestDTO, ServerWebExchange exchange) {
        return userRequestDTO
                .flatMap(userService::registerUser)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }
}
