package br.com.hahn.toxicbet.domain.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("odds")
@RequiredArgsConstructor
public class Odds {

    @Id
    private Long id;

    @Column("match_id")
    private Long matchId;

    @Column("total_bets_for_match")
    private Integer totalBetsForMatch;

    @Column("total_bets_home_win")
    private Integer totalBetsHomeWin;

    @Column("total_bets_visiting_win")
    private Integer totalBetsVisitingWin;

    @Column("total_bets_draw")
    private Integer totalBetsDraw;

    @Column("odd_home_team")
    private Double oddHomeTeam;

    @Column("odd_visiting_team")
    private Double oddVisitingTeam;

    @Column("odd_draw")
    private Double oddDraw;
}
