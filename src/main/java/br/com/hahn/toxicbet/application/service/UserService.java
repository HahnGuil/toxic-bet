package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.application.mapper.UserMapper;
import br.com.hahn.toxicbet.domain.exception.UserNotFoundException;
import br.com.hahn.toxicbet.domain.model.Users;
import br.com.hahn.toxicbet.domain.model.enums.ErrorMessages;
import br.com.hahn.toxicbet.domain.repository.UserRepository;
import br.com.hahn.toxicbet.model.UserRequestDTO;
import br.com.hahn.toxicbet.model.UserResponseDTO;
import br.com.hahn.toxicbet.util.DateTimeConverter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

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

    /**
     * Retrieves the UUID of a user based on their email address.
     *
     * <p>
     * This method performs the following steps:
     * <ol>
     *     <li>Logs the start of the user search process with the provided email and current timestamp.</li>
     *     <li>Attempts to find the user in the repository by their email address.</li>
     *     <li>If a user is found, retrieves their UUID and logs the success.</li>
     *     <li>If no user is found, returns a {@link Mono} error with a {@link UserNotFoundException}.</li>
     *     <li>If an error occurs during the process, logs the error and rethrows it.</li>
     * </ol>
     * </p>
     *
     * @param email the email address of the user to be retrieved.
     * @return a {@link Mono} emitting the {@link UUID} of the user if found, or an error if not found.
     *
     * @author HahnGuil
     */
    public Mono<UUID> findUserByEmail(String email){
        return Mono.defer(() -> {
            log.info("UserService: Find User by email: {}, at: {}", email, DateTimeConverter.formatInstantNow());
            return userRepository.findByEmail(email)
                    .map(Users::getId)
                    .doOnSuccess(uuid -> log.info("UserService: UUID: {} found for the email: {} at: {}", uuid, email, DateTimeConverter.formatInstantNow()))
                    .switchIfEmpty(Mono.error(new UserNotFoundException(ErrorMessages.USER_NOT_FOUND.getMessage() + email)))
                    .doOnError(error -> log.error("UserService: Not found user for this email: {}. Throw the UserNotFoundException at: {}", email, DateTimeConverter.formatInstantNow()));
        });
    }
}