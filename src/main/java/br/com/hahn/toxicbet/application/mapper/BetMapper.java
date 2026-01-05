package br.com.hahn.toxicbet.application.mapper;

import br.com.hahn.toxicbet.domain.model.Bet;
import br.com.hahn.toxicbet.domain.model.enums.Result;
import br.com.hahn.toxicbet.model.BetRequestDTO;
import br.com.hahn.toxicbet.model.BetResponseDTO;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class BetMapper {

    /**
     * Converts a {@link BetRequestDTO} and user ID into a {@link Bet} entity.
     *
     * <p>
     * This method performs the following steps:
     * <ol>
     *     <li>Checks if the provided {@link BetRequestDTO} is null and returns null if true</li>
     *     <li>Creates a new {@link Bet} entity</li>
     *     <li>Sets the user ID, match ID, and result on the {@link Bet} entity</li>
     * </ol>
     * </p>
     *
     * @author HahnGuil
     * @param requestDTO the {@link BetRequestDTO} containing the bet details
     * @param userId the {@link UUID} of the user placing the bet
     * @return a {@link Bet} entity populated with the provided details, or null if the {@link BetRequestDTO} is null
     */
    public Bet toEntity(BetRequestDTO requestDTO, UUID userId){
        if(requestDTO == null) return null;
        Bet bet = new Bet();
        bet.setUserId(userId);
        bet.setMatchId(requestDTO.getMatchId());
        bet.setResult(Result.valueOf(requestDTO.getResult().name()));
        return bet;
    }

    /**
     * Converts a {@link Bet} entity into a {@link BetResponseDTO}.
     *
     * <p>
     * This method performs the following steps:
     * <ol>
     *     <li>Checks if the provided {@link Bet} entity is null and returns null if true</li>
     *     <li>Creates a new {@link BetResponseDTO}</li>
     *     <li>Sets the bet ID, match ID, result, and odds on the {@link BetResponseDTO}</li>
     * </ol>
     * </p>
     *
     * @author HahnGuil
     * @param bet the {@link Bet} entity to be converted
     * @return a {@link BetResponseDTO} populated with the details of the {@link Bet} entity, or null if the {@link Bet} is null
     */
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