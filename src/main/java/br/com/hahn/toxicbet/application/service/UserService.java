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
}
