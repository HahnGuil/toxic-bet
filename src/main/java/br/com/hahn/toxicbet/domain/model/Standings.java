package br.com.hahn.toxicbet.domain.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@Table("standings")
@RequiredArgsConstructor
public class Standings {

    @Id
    private Long id;

    @NotNull
    @Column("user_id")
    private UUID userId;

    @Column("points")
    private Integer points;
}
