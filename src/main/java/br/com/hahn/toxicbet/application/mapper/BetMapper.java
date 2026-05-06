package br.com.hahn.toxicbet.application.mapper;

import br.com.hahn.toxicbet.domain.model.Bet;
import br.com.hahn.toxicbet.domain.model.enums.Result;
import br.com.hahn.toxicbet.model.BetRequestDTO;
import br.com.hahn.toxicbet.model.BetResponseDTO;
import br.com.hahn.toxicbet.model.BetResultResponseDTO;
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

    public BetResultResponseDTO toApiBetResultResponse(br.com.hahn.toxicbet.domain.model.dto.BetResultResponseDTO source) {
        if (source == null) return null;
        return new BetResultResponseDTO()
                .homeTeamName(source.homeTeamName())
                .visitingTeamName(source.visitingTeamName())
                .matchResult(toApiMatchResult(source.matchResult()))
                .betResult(toApiBetResult(source.betResult()))
                .betOdds(source.betOdds())
                .betPoints(source.betPoints())
                .homeTeamScore(source.homeTeamScore())
                .visitingTeamScore(source.visitingTeamScore())
                .resultado(toApiResultado(source.resultado()));
    }

    private BetResultResponseDTO.MatchResultEnum toApiMatchResult(Result result) {
        return result == null ? null : BetResultResponseDTO.MatchResultEnum.fromValue(result.name());
    }

    private BetResultResponseDTO.BetResultEnum toApiBetResult(Result result) {
        return result == null ? null : BetResultResponseDTO.BetResultEnum.fromValue(result.name());
    }

    private BetResultResponseDTO.ResultadoEnum toApiResultado(Result result) {
        return result == null ? null : BetResultResponseDTO.ResultadoEnum.fromValue(result.name());
    }
}
