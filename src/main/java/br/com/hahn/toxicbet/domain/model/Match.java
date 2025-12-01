package br.com.hahn.toxicbet.domain.model;

import br.com.hahn.toxicbet.domain.model.enums.Result;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Table("match")
public class Match {

    @Id
    private Long id;
    private Long homeTeamId;
    private Long visitingTeamId;
    private Result result;
    private LocalDateTime matchTime;


}
