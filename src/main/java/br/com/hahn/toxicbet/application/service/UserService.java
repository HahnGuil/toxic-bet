package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.application.mapper.UserMapper;
import br.com.hahn.toxicbet.domain.repository.UserRepository;
import br.com.hahn.toxicbet.model.UserRequestDTO;
import br.com.hahn.toxicbet.model.UserResponseDTO;
import br.com.hahn.toxicbet.util.DateTimeConverter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service responsible for handling user-related operations.
 *
 * @author HahnGuil
 */
@Service
@AllArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * Registers a new user in the application.
     * Converts the user request DTO to an entity, saves it in the repository, and maps the result back to a response DTO.
     * Logs the process at various stages and handles errors gracefully.
     *
     * @param userRequestDTO The data transfer object containing the user information to be registered.
     * @return A {@link Mono} containing the {@link UserResponseDTO} with the registered user data.
     *
     * @author HahnGuil
     */
    public Mono<UserResponseDTO> registerUser(UserRequestDTO userRequestDTO) {
        return Mono.defer(() -> {
            log.info("Starting user registration in the application for email: {} at: {}",
                    userRequestDTO.getEmail(), DateTimeConverter.formatInstantNow());
            return userRepository.save(userMapper.toEntity(userRequestDTO))
                    .doOnSuccess(saved -> log.info("User with email: {}, successfully registered at: {}",
                            userRequestDTO.getEmail(), DateTimeConverter.formatInstantNow()))
                    .doOnError(err -> log.error("Error registering user with email: {}, error: {}, at: {}",
                            userRequestDTO.getEmail(), err.getMessage(), DateTimeConverter.formatInstantNow()))
                    .map(userMapper::toResponseDTO);
        });
    }
}