package br.com.hahn.toxicbet.domain.model;

import br.com.hahn.toxicbet.domain.model.enums.Result;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("Bet")
public class Bet {

    @Id
    private Long id;
    private Long userId;
    private Long matchId;
    private Result result;
}
