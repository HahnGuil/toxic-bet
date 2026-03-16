package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.application.mapper.UserMapper;
import br.com.hahn.toxicbet.domain.exception.NotFoundException;
import br.com.hahn.toxicbet.domain.model.Users;
import br.com.hahn.toxicbet.domain.model.dto.UserDTO;
import br.com.hahn.toxicbet.domain.model.enums.ErrorMessages;
import br.com.hahn.toxicbet.domain.repository.BetRepository;
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
    private final BetRepository betRepository;


    public Mono<UserResponseDTO> registerUser(UserRequestDTO userRequestDTO) {
            return userRepository.save(userMapper.toEntity(userRequestDTO))
                    .map(userMapper::toDTO);
    }

    public Mono<UUID> findUserByEmail(String email){
            return userRepository.findByEmail(email)
                    .map(Users::getId)
                    .switchIfEmpty(Mono.defer(() -> {
                        log.error("UserService: NOT_FOUND: Not found user with email: {}. Throw Not Found Exception at: {}", email, DateTimeConverter.formatInstantNow());
                        return Mono.error(new NotFoundException(ErrorMessages.USER_NOT_FOUND.getMessage()));
                    }));
    }

    public Mono<Void> calculatedUserPoints(Long matchId, String result){
        return betRepository.findByMatchId(matchId)
                .filter(bet -> result.equals(bet.getResult().name()))
                .flatMap(bet -> userRepository.findById(bet.getUserId())
                        .flatMap(users -> {
                            users.setUserPoints(users.getUserPoints() + bet.getUserPoint());
                            return userRepository.save(users);
                        })
                ).then();
    }

    public Mono<Users> findById(UUID userId){
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("UserService: NOT_FOUND: Not found user with id: {}. Throw Not Found Exception at: {}", userId, DateTimeConverter.formatInstantNow());
                    return Mono.error(new NotFoundException(ErrorMessages.USER_NOT_FOUND.getMessage()));
                }));
    }

    public Mono<UserResponseDTO> getUser(UserDTO userDTO){
        if (userDTO.userEmail() != null){
            return userRepository.findByEmail(userDTO.userEmail()).map(userMapper::toDTO);
        }
        return userRepository.findById(userDTO.userId()).map(userMapper::toDTO);
    }

    public Mono<Boolean> existsByEmail(String email){
        return userRepository.existsByEmail(email);
    }

}