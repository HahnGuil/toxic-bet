package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.application.mapper.UserMapper;
import br.com.hahn.toxicbet.domain.exception.NotAuthorizedException;
import br.com.hahn.toxicbet.domain.exception.NotFoundException;
import br.com.hahn.toxicbet.domain.model.Users;
import br.com.hahn.toxicbet.domain.model.dto.UserDTO;
import br.com.hahn.toxicbet.domain.model.enums.ErrorMessages;
import br.com.hahn.toxicbet.domain.model.enums.Role;
import br.com.hahn.toxicbet.domain.repository.BetRepository;
import br.com.hahn.toxicbet.domain.repository.UserRepository;
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
    private final AuthServiceRegistrationService authServiceRegistrationService;

    public Mono<Void> registerUser(String userName, String userEmail, String authorizationHeader) {
        return userRepository.save(userMapper.toEntity(userName, userEmail))
                .doOnNext(savedUser -> log.info("User registered successfully with email: {}", savedUser.getEmail()))
                .then(authServiceRegistrationService.registerService(authorizationHeader))
                .then();
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
                            users.setUserPoints((users.getUserPoints() != null ? users.getUserPoints() : 0.0) + bet.getUserPoint());
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

    public Mono<Users> findByEmail(String email){
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("UserService: NOT_FOUND: Not found user with email: {}.  Throw Not Found Exception at: {}", email, DateTimeConverter.formatInstantNow());
                    return Mono.error(new NotFoundException(ErrorMessages.USER_NOT_FOUND.getMessage()));
                }));
    }

    public Mono<UserResponseDTO> getUser(UserDTO userDTO){
        if (userDTO.userEmail() != null){
            return userRepository.findByEmail(userDTO.userEmail()).map(userMapper::toDTO);
        }
        return userRepository.findById(userDTO.userId()).map(userMapper::toDTO);
    }

    public Mono<Void> isUserAdmin(String email){
        return userRepository.findByEmail(email)
                .flatMap(user -> {
                    if (!Role.ADMIN.equals(user.getRole())) {
                        log.error("MatchService: UNAUTHORIZED: User {} does not have ADMIN role at: {}", email, DateTimeConverter.formatInstantNow());
                        return Mono.error(new NotAuthorizedException(ErrorMessages.FORBIDDEN_OPERATION.getMessage()));
                    }
                    return Mono.empty();
                }).then();
    }

    public Mono<Void> updateUserRole(String userEmail){
        return userRepository.findByEmail(userEmail)
                .flatMap(users -> {
                    log.info("UserService: Role ADMIN defines to user: {}, at: {}", users.getName(), DateTimeConverter.formatInstantNow());
                    users.setRole(Role.ADMIN);
                    return userRepository.save(users);
                }).then();
    }

    public Mono<Boolean> existsByEmail(String email){
        return userRepository.existsByEmail(email);
    }
}