package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.application.mapper.BetPoolMapper;
import br.com.hahn.toxicbet.domain.exception.NotFoundException;
import br.com.hahn.toxicbet.domain.model.BettingPool;
import br.com.hahn.toxicbet.domain.model.Users;
import br.com.hahn.toxicbet.domain.model.dto.BettingPoolDTO;
import br.com.hahn.toxicbet.domain.model.enums.ErrorMessages;
import br.com.hahn.toxicbet.domain.repository.BettingPoolRepository;
import br.com.hahn.toxicbet.model.BettingPoolRequestDTO;
import br.com.hahn.toxicbet.model.BettingPoolResponseDTO;
import br.com.hahn.toxicbet.model.BettingPoolUserPointsDTO;
import br.com.hahn.toxicbet.model.BettingPoolUsersResponseDTO;
import br.com.hahn.toxicbet.model.SuccessResponseDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import br.com.hahn.toxicbet.util.DateTimeConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BettingPoolService {

    private final BettingPoolRepository bettingPoolRepository;
    private final BetPoolMapper betPoolMapper;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public Mono<BettingPoolResponseDTO> createBettingPool(Mono<BettingPoolRequestDTO> requestDTOMono, Mono<String> userEmailMono) {
        return requestDTOMono
                .zipWith(findUserIdByEmail(userEmailMono))
                .doOnSubscribe(subscription -> log.info("BettingPoolService: Starting betting pool creation"))
                .flatMap(tuple ->
                        generateKeyForBettingPool()
                                .map(key -> new BettingPoolDTO(
                                        tuple.getT2(),
                                        tuple.getT1().getBettingPoolName(),
                                        key
                                ))
                )
                .map(betPoolMapper::toEntity)
                .flatMap(bettingPoolRepository::save)
                .map(betPoolMapper::toDTO)
                .doOnError(error -> log.error("BettingPoolService: Error while creating betting pool: {}", error.getMessage(), error));
    }


    public Mono<BettingPoolResponseDTO> getBettingPoolByKey(String bettingPoolKey){
        return bettingPoolRepository.findBettingPoolByBettingPoolKey(bettingPoolKey)
                .map(betPoolMapper::toDTO);
    }

    public Mono<SuccessResponseDTO> addUserToBettingPool(String bettingPoolKey, Mono<String> userEmailMono){
        return findByBettingPoolKey(bettingPoolKey)
                .zipWith(findUserIdByEmail(userEmailMono))
                .doOnSubscribe(subscription ->
                        log.info("BettingPoolService: Add user: {} to bettingPool with code: {}", userEmailMono, bettingPoolKey))
                .flatMap(tuple -> {
                    var bettingPool = tuple.getT1();
                    var userId = tuple.getT2().toString();

                    List<String> users = bettingPool.getUserIds();
                    users.add(userId);
                    bettingPool.setUserIds(users);

                    return bettingPoolRepository.save(bettingPool)
                            .thenReturn(new SuccessResponseDTO().message(
                                    "User Added successfully for Betting Pool: " + bettingPool.getBettingPoolName()))
                            .doOnError(error -> log.error("BettingPoolService: Error while add user to bettingPool: {}", error.getMessage(), error));
                });
    }

    public Mono<SuccessResponseDTO> removeUserFromBettingPool(String bettingPoolKey, Mono<String> userEmailMono){
        return findByBettingPoolKey(bettingPoolKey)
                .zipWith(findUserIdByEmail(userEmailMono))
                .doOnSubscribe(subscription ->
                        log.info("BettingPoolService: Remove user from bettingPool with code: {}", bettingPoolKey))
                .flatMap(tuple -> {
                    var bettingPool = tuple.getT1();
                    var userId = tuple.getT2().toString();

                    List<String> users = new ArrayList<>(Optional.ofNullable(bettingPool.getUserIds()).orElseGet(ArrayList::new));
                    users.removeIf(userId::equals);
                    bettingPool.setUserIds(users);

                    return bettingPoolRepository.save(bettingPool)
                            .thenReturn(new SuccessResponseDTO().message(
                                    "User removed successfully from Betting Pool: " + bettingPool.getBettingPoolName()))
                            .doOnError(error -> log.error("BettingPoolService: Error while removing user from bettingPool: {}", error.getMessage(), error));
                });
    }

    private Mono<UUID> findUserIdByEmail(Mono<String> userEmailMono) {
        return userEmailMono.flatMap(userService::findUserByEmail);
    }

    public Flux<BettingPoolUsersResponseDTO> getBettingPoolUsers(Mono<String> userEmailMono) {
        return findUserIdByEmail(userEmailMono)
                .map(UUID::toString)
                .flatMapMany(bettingPoolRepository::findAllByUserId)
                .doOnSubscribe(subscription -> log.info("BettingPoolService: Listing betting pools for authenticated user"))
                .flatMap(this::toBettingPoolUsersResponse);
    }

    private Mono<BettingPoolUsersResponseDTO> toBettingPoolUsersResponse(BettingPool bettingPool) {
        return Flux.fromIterable(Optional.ofNullable(bettingPool.getUserIds()).orElseGet(ArrayList::new))
                .map(UUID::fromString)
                .flatMap(userService::findById)
                .map(this::toUserPointsDTO)
                .collectSortedList(Comparator.comparing(BettingPoolUserPointsDTO::getPoints, Comparator.nullsFirst(Double::compareTo)).reversed())
                .map(users -> betPoolMapper.toBettingPoolUserDTO(bettingPool, users));
    }

    private Mono<BettingPool> findByBettingPoolKey(String bettingPoolKey){
        return bettingPoolRepository.findBettingPoolByBettingPoolKey(bettingPoolKey)
                .switchIfEmpty(Mono.error(new NotFoundException(ErrorMessages.BETTING_POOL_NOT_FOUND_KEY.getMessage())))
                .doOnError(erro ->
                        log.error("BettingPoolService: NOT_FOUND. Not found bettingPool with key: {}. Throw NotFoundExcepton", bettingPoolKey));
    }

    private BettingPoolUserPointsDTO toUserPointsDTO(Users users) {
        return new BettingPoolUserPointsDTO()
                .userName(extractReadableUserName(users.getName()))
                .points(users.getUserPoints() == null ? 0.0 : users.getUserPoints());
    }

    private String extractReadableUserName(String rawUserName) {
        if (rawUserName == null || rawUserName.isBlank()) {
            return "Unknown User";
        }
        String trimmedName = rawUserName.trim();

        if (!trimmedName.startsWith("{")) {
            return trimmedName;
        }

        try {
            JsonNode node = objectMapper.readTree(trimmedName);
            if (node.hasNonNull("userName")) {
                return node.get("userName").asText();
            }
            if (node.hasNonNull("name")) {
                return node.get("name").asText();
            }
        } catch (Exception exception) {
            log.debug("BettingPoolService: Could not parse user name payload, returning raw value: {}", trimmedName);
        }

        return trimmedName;
    }

    private Mono<String> generateKeyForBettingPool() {
        return bettingPoolRepository
                .totalBettingPool()
                .map(total -> total + 1)
                .map(total -> String.format("%02d", total) + DateTimeConverter.getMouthAndYear());
    }
}
