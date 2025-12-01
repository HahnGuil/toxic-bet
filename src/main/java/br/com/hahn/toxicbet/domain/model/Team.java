package br.com.hahn.toxicbet.domain.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("teams")
public class Team {

    @Id
    private Long id;
    private String name;
}
