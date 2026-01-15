package br.com.hahn.toxicbet.application.mapper;

import br.com.hahn.toxicbet.domain.model.Bet;
import br.com.hahn.toxicbet.domain.model.enums.Result;
import br.com.hahn.toxicbet.model.BetRequestDTO;
import br.com.hahn.toxicbet.model.BetResponseDTO;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class BetMapper {

    public Bet toEntity(BetRequestDTO requestDTO, UUID userId){
        if(requestDTO == null) return null;
        Bet bet = new Bet();
        bet.setUserId(userId);
        bet.setMatchId(requestDTO.getMatchId());
        bet.setResult(Result.valueOf(requestDTO.getResult().name()));
        return bet;
    }

    public BetResponseDTO toDTO(Bet bet){
        if(bet == null) return  null;
        var betResponseDTO = new BetResponseDTO();
        betResponseDTO.setBetId(bet.getId());
        betResponseDTO.setMatchId(bet.getMatchId());
        betResponseDTO.setResult(String.valueOf(bet.getResult()));
        betResponseDTO.setOdds(bet.getBetOdds());
        return betResponseDTO;
    }
}