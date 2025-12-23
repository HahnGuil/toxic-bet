package br.com.hahn.toxicbet.domain.model;

import br.com.hahn.toxicbet.domain.model.enums.Result;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Table("match")
@RequiredArgsConstructor
public class Match {

    @Id
    private Long id;

    @Column("home_team_id")
    private Long homeTeamId;

    @Column("visiting_team_id")
    private Long visitingTeamId;

    @Column("home_team_score")
    private Integer homeTeamScore;

    @Column("visiting_team_score")
    private Integer visitingTeamScore;

    @Column("result")
    private Result result;

    @Column("match_time")
    private LocalDateTime matchTime;
}