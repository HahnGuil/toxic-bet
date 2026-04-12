package br.com.hahn.toxicbet.application.mapper;

import br.com.hahn.toxicbet.domain.model.Championship;
import br.com.hahn.toxicbet.model.ChampionshipDTO;
import br.com.hahn.toxicbet.model.ChampionshipRequestDTO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChampionshipMapper {

    public Championship toEntity(ChampionshipRequestDTO requestDTO){
        if(requestDTO == null) return null;

        Championship championship = new Championship();
        championship.setName(requestDTO.getName());
        return championship;
    }

    public ChampionshipDTO toDTO(Championship championship){
        if(championship == null) return null;

        ChampionshipDTO championshipDTO = new ChampionshipDTO();
        championshipDTO.setIdChampionship(championship.getId());
        championshipDTO.setName(championship.getName());

        return championshipDTO;
    }


}
