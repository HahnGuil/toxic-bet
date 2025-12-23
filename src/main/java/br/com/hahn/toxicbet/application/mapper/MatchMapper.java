package br.com.hahn.toxicbet.application.mapper;

import br.com.hahn.toxicbet.domain.model.Match;
import br.com.hahn.toxicbet.model.MatchRequestDTO;
import br.com.hahn.toxicbet.model.MatchResponseDTO;
import br.com.hahn.toxicbet.util.DateTimeConverter;
import org.springframework.stereotype.Component;

/**
 * Mapper class responsible for converting between Match-related DTOs and entity objects.
 * This class provides methods to map data from MatchRequestDTO to Match entity
 * and from Match entity to MatchResponseDTO.
 *
 * @author HahnGuil
 */
@Component
public class MatchMapper {

    /**
     * Converts a MatchRequestDTO object to a Match entity.
     *
     * @author HahnGuil
     * @param dto the MatchRequestDTO object to be converted
     * @return the corresponding Match entity, or null if the input is null
     */
    public Match toEntity(MatchRequestDTO dto){
        if(dto == null) return null;
        Match match = new Match();
        match.setHomeTeamId(dto.getHomeTeamId());
        match.setVisitingTeamId(dto.getVisitingTeamId());
        match.setMatchTime(DateTimeConverter.parseToLocalDateTime(dto.getMatchTime()));
        return match;
    }

    /**
     * Converts a Match entity to a MatchResponseDTO object.
     *
     * @author HahnGuil
     * @param match the Match entity to be converted
     * @param homeTeamName the name of the home team
     * @param visitingTeamName the name of the visiting team
     * @return the corresponding MatchResponseDTO object, or null if the input is null
     */
    public MatchResponseDTO toDto(Match match, String homeTeamName, String visitingTeamName) {
        if (match == null) return null;

        MatchResponseDTO dto = new MatchResponseDTO();
        dto.setMatchId(match.getId());
        dto.setHomeTeamName(homeTeamName);
        dto.setVisitingTeamName(visitingTeamName);
        dto.setMatchTime(DateTimeConverter.formatLocalDateTime(match.getMatchTime()));
        dto.setResult(MatchResponseDTO.ResultEnum.valueOf(match.getResult().toString()));
        return dto;
    }
}