package br.com.hahn.toxicbet.application.mapper;

import br.com.hahn.toxicbet.domain.model.BettingPool;
import br.com.hahn.toxicbet.domain.model.Users;
import br.com.hahn.toxicbet.domain.model.dto.BettingPoolDTO;
import br.com.hahn.toxicbet.model.BettingPoolResponseDTO;
import br.com.hahn.toxicbet.model.BettingPoolUsersResponseDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class BetPoolMapper {

    private BetPoolMapper(){}

    public BettingPool toEntity(BettingPoolDTO dto) {
        if(dto == null) return null;

        BettingPool bettingPool = new BettingPool();
        bettingPool.setBettingPoolOwnerId(dto.betPoolOwnerId());
        bettingPool.setBettingPoolName(dto.betPoolName());
        bettingPool.setBettingPoolKey(dto.betPoolKey());
        List<String> users = new ArrayList<>();
        users.add(dto.betPoolOwnerId().toString());
        bettingPool.setUserIds(users);

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

    public BettingPoolUsersResponseDTO toBettingPoolUserDTO(BettingPool bettingPool, Map<String, Double> users){
        BettingPoolUsersResponseDTO usersBettingPool = new BettingPoolUsersResponseDTO();
        usersBettingPool.bettingPoolId(bettingPool.getId());
        usersBettingPool.bettingPoolKey(bettingPool.getBettingPoolKey());
        usersBettingPool.setUsers(users);

        return usersBettingPool;

    }
}
