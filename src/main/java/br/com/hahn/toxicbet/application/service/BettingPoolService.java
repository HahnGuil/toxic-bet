package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.application.mapper.BetPoolMapper;
import br.com.hahn.toxicbet.domain.repository.BettingPoolRepository;
import br.com.hahn.toxicbet.model.BettingPoolResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class BettingPoolService {

    private final BettingPoolRepository bettingPoolRepository;
    private final BetPoolMapper betPoolMapper;

    public Mono<BettingPoolResponseDTO> getBettingPoolByKey(String bettingPoolKey){
        return bettingPoolRepository.findBettingPoolByBettingPoolKey(bettingPoolKey)
                .map(betPoolMapper::toDTO);
    }

//    public Mono<String> addUserToBettingPool(String bettingPoolKey, String userId){
//        return bettingPoolRepository.findBettingPoolByBettingPoolKey(bettingPoolKey)
//                .flatMap(bettingPool -> bettingPool.)
//    }
}
