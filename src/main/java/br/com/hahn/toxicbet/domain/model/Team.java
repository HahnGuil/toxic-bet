package br.com.hahn.toxicbet.domain.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("teams")
@RequiredArgsConstructor
public class Team {

    @Id
    private Long id;

    @NotNull
    @Column("name")
    private String name;
}
