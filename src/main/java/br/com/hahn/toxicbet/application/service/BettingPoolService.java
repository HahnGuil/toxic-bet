package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.application.mapper.BetPoolMapper;
import br.com.hahn.toxicbet.domain.exception.NotFoundException;
import br.com.hahn.toxicbet.domain.model.Users;
import br.com.hahn.toxicbet.domain.model.dto.BettingPoolDTO;
import br.com.hahn.toxicbet.domain.model.enums.ErrorMessages;
import br.com.hahn.toxicbet.domain.repository.BettingPoolRepository;
import br.com.hahn.toxicbet.model.BettingPoolRequestDTO;
import br.com.hahn.toxicbet.model.BettingPoolResponseDTO;
import br.com.hahn.toxicbet.model.BettingPoolUsersResponseDTO;
import br.com.hahn.toxicbet.model.SuccessResponseDTO;
import br.com.hahn.toxicbet.util.DateTimeConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BettingPoolService {

    private final BettingPoolRepository bettingPoolRepository;
    private final BetPoolMapper betPoolMapper;
    private final UserService userService;

    public Mono<BettingPoolResponseDTO> createBettingPool(Mono<BettingPoolRequestDTO> requestDTOMono, Mono<String> userId) {
        return requestDTOMono
                .flatMap(requestDTO ->
                        generateKeyForBettingPool()
                                .map(key -> new BettingPoolDTO(
                                        UUID.fromString(String.valueOf(userId)),
                                        key,
                                        requestDTO.getBettingPoolName()
                                ))
                )
                .map(betPoolMapper::toEntity)
                .flatMap(bettingPoolRepository::save)
                .map(pool -> {
                    log.info("BettingPoolService: ");
                    return pool;
                })
                .map(betPoolMapper::toDTO);
    }

    public Mono<BettingPoolResponseDTO> getBettingPoolByKey(String bettingPoolKey){
        return bettingPoolRepository.findBettingPoolByBettingPoolKey(bettingPoolKey)
                .map(betPoolMapper::toDTO);
    }

    public Mono<SuccessResponseDTO> addUserToBettingPool(String bettingPoolKey, Mono<String> userId){
        return bettingPoolRepository.findBettingPoolByBettingPoolKey(bettingPoolKey)
                .switchIfEmpty(Mono.error(new NotFoundException(ErrorMessages.BETTING_POOL_NOT_FOUND_KEY.getMessage())))
                .flatMap(bettingPool -> {
                    List<String> users = bettingPool.getUserIds();
                    users.add(String.valueOf(userId));
                    bettingPool.setUserIds(users);
                    return bettingPoolRepository.save(bettingPool)
                            .thenReturn(new SuccessResponseDTO().message("User Added successfully for Betting Pool: " + bettingPool.getBettingPoolName()));
                });
    }

    public Mono<BettingPoolUsersResponseDTO> getBettingPoolUsers(String bettingPoolKey){
        return bettingPoolRepository.findBettingPoolByBettingPoolKey(bettingPoolKey)
                .flatMap(bettingPool ->
                        Flux.fromIterable(bettingPool.getUserIds())
                                .map(UUID::fromString)
                                .flatMap(userService::findById)
                                .map(this::toUserEntity)
                                .collectList()
                                .map(entries -> entries.stream()
                                        .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.nullsFirst(Double::compareTo)).reversed())
                                        .collect(Collectors.toMap(
                                                Map.Entry::getKey,
                                                Map.Entry::getValue,
                                                (a, b) -> a,
                                                LinkedHashMap::new
                                        )))
                                .map(usersMap -> betPoolMapper.toBettingPoolUserDTO(bettingPool, usersMap))
                );

    }

    private Map.Entry<String, Double> toUserEntity(Users users){
        String key = users.getName();
        Double value = users.getUserPoints() == null ? 0.0 : users.getUserPoints();
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    private Mono<String> generateKeyForBettingPool() {
        return bettingPoolRepository
                .totalBettingPool()
                .map(total -> total + 1)
                .map(total -> String.format("%02d", total) + DateTimeConverter.getMouthAndYear());
    }
}
