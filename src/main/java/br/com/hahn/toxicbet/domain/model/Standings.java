package br.com.hahn.toxicbet.domain.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("standings")
public class Standings {

    @Id
    private Long id;
    private Long userId;
    private Integer points;
}
