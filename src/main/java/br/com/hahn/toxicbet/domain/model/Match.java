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

    @Column("odds_home_team")
    private Double oddsHomeTeam;

    @Column("odds_visiting_team")
    private Double oddsVisitingTeam;

    @Column("odds_draw")
    private Double oddsDraw;

    @Column("championship_id")
    private Long championshipId;

    @Column("result")
    private Result result;

    @Column("match_time")
    private LocalDateTime matchTime;

    @Column("ttotal_bet_home_team")
    private Integer totalBetHomeTeam;

    @Column("total_bet_draw")
    private Integer totalBetDraw;

    @Column("total_bet_visiting_team")
    private Integer totalBetVisitingTeam;

    @Column("total_bet_match")
    private Integer totalBetMatch;
}