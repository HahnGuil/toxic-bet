package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.application.mapper.UserMapper;
import br.com.hahn.toxicbet.domain.exception.NotFoundException;
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
            return userRepository.save(userMapper.toEntity(userRequestDTO))
                    .map(userMapper::toResponseDTO);
    }

    public Mono<UUID> findUserByEmail(String email){
            return userRepository.findByEmail(email)
                    .map(Users::getId)
                    .switchIfEmpty(Mono.defer(() -> {
                        log.error("UserService: NOT_FOUND: Not found user with email: {}. Throw Not Found Exception at: {}", email, DateTimeConverter.formatInstantNow());
                        return Mono.error(new NotFoundException(ErrorMessages.USER_NOT_FOUND.getMessage()));
                    }));
    }

    public Mono<Long> countAllUsers(){
        return userRepository.count();
    }
}