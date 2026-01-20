package br.com.hahn.toxicbet.application.mapper;

import br.com.hahn.toxicbet.domain.model.Match;
import br.com.hahn.toxicbet.model.MatchRequestDTO;
import br.com.hahn.toxicbet.model.MatchResponseDTO;
import br.com.hahn.toxicbet.util.DateTimeConverter;
import org.springframework.stereotype.Component;

@Component
public class MatchMapper {

    public Match toEntity(MatchRequestDTO dto){
        if(dto == null) return null;
        Match match = new Match();
        match.setHomeTeamId(dto.getHomeTeamId());
        match.setVisitingTeamId(dto.getVisitingTeamId());
        match.setMatchTime(DateTimeConverter.parseToLocalDateTime(dto.getMatchTime()));
        return match;
    }

    public MatchResponseDTO toDto(Match match, String homeTeamName, String visitingTeamName, String championshipName) {
        if (match == null) return null;

        MatchResponseDTO dto = new MatchResponseDTO();
        dto.setMatchId(match.getId());
        dto.setHomeTeamName(homeTeamName);
        dto.setVisitingTeamName(visitingTeamName);
        dto.setMatchTime(DateTimeConverter.formatLocalDateTime(match.getMatchTime()));
        dto.setHomeTeamScore(match.getHomeTeamScore());
        dto.setVisitingTeamScore(match.getVisitingTeamScore());
        dto.setChampionshipName(championshipName);
        dto.setResult(MatchResponseDTO.ResultEnum.valueOf(match.getResult().toString()));
        return dto;
    }
}