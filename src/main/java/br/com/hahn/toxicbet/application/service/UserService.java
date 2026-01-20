package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.application.mapper.UserMapper;
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

@Service
@AllArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

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

    public Mono<Long> countAllUsers(){
        return Mono.defer(() -> {
            log.info("UserService: Get counting all users at: {}", DateTimeConverter.formatInstantNow());
            return userRepository.count()
                    .doOnSuccess(count -> log.info("UserService: Total users count: {}: at: {}", count, DateTimeConverter.formatInstantNow()))
                    .doOnError(error -> log.error("UserService: Error counting users: {}, at: {}", error.getMessage(), DateTimeConverter.formatInstantNow()));
        });
    }
}