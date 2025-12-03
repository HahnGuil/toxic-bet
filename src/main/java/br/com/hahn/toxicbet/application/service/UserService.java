package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.domain.model.Users;
import br.com.hahn.toxicbet.domain.repository.UserRepository;
import br.com.hahn.toxicbet.model.UserRequestDTO;
import br.com.hahn.toxicbet.model.UserResponseDTO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    public Mono<UserResponseDTO> registerUser(UserRequestDTO userRequestDTO){
        var user = new Users();
        user.setName(userRequestDTO.getName());
        user.setEmail(userRequestDTO.getEmail());

        return userRepository.save(user).map(savedUser -> {
                var response = new UserResponseDTO();
                    response.setMessage("User successfully registered");
                    return response;
                })
                .doOnSuccess(u -> log.info("User registered successfully: {}", userRequestDTO.getEmail()))
                .doOnError(e -> log.error("Error registering user: {}", e.getMessage()));
    }
}
