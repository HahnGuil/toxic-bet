package br.com.hahn.toxicbet.application.mapper;

import br.com.hahn.toxicbet.domain.model.Team;
import br.com.hahn.toxicbet.model.TeamRequestDTO;
import br.com.hahn.toxicbet.model.TeamResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class TeamMapper {

    public Team toEntity(TeamRequestDTO dto){
        if(dto == null) return null;

        Team team = new Team();
        team.setName(dto.getName());
        return team;
    }

    public TeamResponseDTO toDTO(Team team){
        if(team == null) return null;

        TeamResponseDTO responseDTO = new TeamResponseDTO();
        responseDTO.setName(team.getName());

        return responseDTO;
    }
}
