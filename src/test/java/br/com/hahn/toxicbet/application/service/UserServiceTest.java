package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.application.mapper.UserMapper;
import br.com.hahn.toxicbet.domain.exception.NotFoundException;
import br.com.hahn.toxicbet.domain.model.Bet;
import br.com.hahn.toxicbet.domain.model.Users;
import br.com.hahn.toxicbet.domain.model.dto.UserDTO;
import br.com.hahn.toxicbet.domain.model.enums.Result;
import br.com.hahn.toxicbet.domain.repository.BetRepository;
import br.com.hahn.toxicbet.domain.repository.UserRepository;
import br.com.hahn.toxicbet.model.UserRequestDTO;
import br.com.hahn.toxicbet.model.UserResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private BetRepository betRepository;

    @InjectMocks
    private UserService service;

    @Test
    void shouldRegisterUser() {
        UserRequestDTO request = new UserRequestDTO();
        request.setName("Alice");
        request.setEmail("alice@test.com");

        Users entity = new Users();
        entity.setName("Alice");
        entity.setEmail("alice@test.com");

        UserResponseDTO response = new UserResponseDTO();
        response.setUserName("Alice");
        response.setUserEmail("alice@test.com");

        when(userMapper.toEntity(request)).thenReturn(entity);
        when(userRepository.save(entity)).thenReturn(Mono.just(entity));
        when(userMapper.toDTO(entity)).thenReturn(response);

        StepVerifier.create(service.registerUser(request))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void shouldFindUserIdByEmail() {
        UUID userId = UUID.randomUUID();
        Users user = new Users();
        user.setId(userId);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Mono.just(user));

        StepVerifier.create(service.findUserByEmail("user@test.com"))
                .expectNext(userId)
                .verifyComplete();
    }

    @Test
    void shouldReturnNotFoundWhenEmailDoesNotExist() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Mono.empty());

        StepVerifier.create(service.findUserByEmail("missing@test.com"))
                .expectError(NotFoundException.class)
                .verify();
    }

    @Test
    void shouldCalculateUserPointsOnlyForMatchingResult() {
        Long matchId = 7L;
        UUID userId = UUID.randomUUID();

        Bet winBet = new Bet();
        winBet.setMatchId(matchId);
        winBet.setUserId(userId);
        winBet.setResult(Result.HOME_WIN);
        winBet.setUserPoint(3.5);

        Bet drawBet = new Bet();
        drawBet.setMatchId(matchId);
        drawBet.setUserId(UUID.randomUUID());
        drawBet.setResult(Result.DRAW);
        drawBet.setUserPoint(9.0);

        Users user = new Users();
        user.setId(userId);
        user.setUserPoints(null);

        when(betRepository.findByMatchId(matchId)).thenReturn(Flux.just(winBet, drawBet));
        when(userRepository.findById(userId)).thenReturn(Mono.just(user));
        when(userRepository.save(any(Users.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.calculatedUserPoints(matchId, Result.HOME_WIN.name()))
                .verifyComplete();

        ArgumentCaptor<Users> captor = ArgumentCaptor.forClass(Users.class);
        verify(userRepository).save(captor.capture());
        assertEquals(3.5, captor.getValue().getUserPoints());
    }

    @Test
    void shouldFindUserById() {
        UUID userId = UUID.randomUUID();
        Users user = new Users();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Mono.just(user));

        StepVerifier.create(service.findById(userId))
                .expectNext(user)
                .verifyComplete();
    }

    @Test
    void shouldReturnNotFoundWhenFindByIdIsEmpty() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Mono.empty());

        StepVerifier.create(service.findById(userId))
                .expectError(NotFoundException.class)
                .verify();
    }

    @Test
    void shouldGetUserByEmailWhenEmailIsProvided() {
        Users user = new Users();
        user.setEmail("mail@test.com");
        UserResponseDTO response = new UserResponseDTO();
        response.setUserEmail("mail@test.com");

        when(userRepository.findByEmail("mail@test.com")).thenReturn(Mono.just(user));
        when(userMapper.toDTO(user)).thenReturn(response);

        StepVerifier.create(service.getUser(new UserDTO(UUID.randomUUID(), "mail@test.com")))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void shouldDelegateExistsByEmail() {
        when(userRepository.existsByEmail("exists@test.com")).thenReturn(Mono.just(true));

        StepVerifier.create(service.existsByEmail("exists@test.com"))
                .expectNext(true)
                .verifyComplete();
    }
}

