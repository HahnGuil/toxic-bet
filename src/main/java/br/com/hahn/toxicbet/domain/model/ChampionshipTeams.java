package br.com.hahn.toxicbet.domain.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("championship_teams")
@RequiredArgsConstructor
public class ChampionshipTeams {

    @Id
    private Long id;

    @NotNull
    @Column("championship_id")
    private Long championshipId;

    @NotNull
    @Column("team_id")
    private Long teamId;
}
