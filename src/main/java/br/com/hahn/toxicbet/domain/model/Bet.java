package br.com.hahn.toxicbet.domain.model;

import br.com.hahn.toxicbet.domain.model.enums.Result;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@Table("Bet")
@RequiredArgsConstructor
public class Bet {

    @Id
    private Long id;

    @Column("user_id")
    private UUID userId;

    @Column("match_id")
    private Long matchId;

    @Column("result")
    private Result result;

    @Column("bet_odds")
    private Double betOdds;
}