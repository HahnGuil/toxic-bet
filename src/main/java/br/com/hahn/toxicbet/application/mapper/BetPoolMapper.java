package br.com.hahn.toxicbet.application.mapper;

import br.com.hahn.toxicbet.domain.model.BettingPool;
import br.com.hahn.toxicbet.domain.model.dto.BettingPoolDTO;
import br.com.hahn.toxicbet.model.BettingPoolResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class BetPoolMapper {

    private BetPoolMapper(){}

    public BettingPool toEntity(BettingPoolDTO dto) {
        if(dto == null) return null;
        BettingPool bettingPool = new BettingPool();
        bettingPool.setBettingPoolOwnerId(dto.betPoolOwnerId());
        bettingPool.setBettingPoolKey(dto.betPoolKey());
        bettingPool.setBettingPoolName(dto.betPoolName());
        return bettingPool;
    }

    public BettingPoolResponseDTO toDTO(BettingPool bettingPool){
        if(bettingPool == null) return null;
        BettingPoolResponseDTO bettingPoolResponseDTO = new BettingPoolResponseDTO();
        bettingPoolResponseDTO.setIdBettingPool(bettingPool.getId());
        bettingPoolResponseDTO.setBettingPoolKey(bettingPool.getBettingPoolKey());
        bettingPoolResponseDTO.setBettingPoolName(bettingPool.getBettingPoolName());
        return bettingPoolResponseDTO;
    }
}
