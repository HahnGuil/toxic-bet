package br.com.hahn.toxicbet.domain.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@Table("users")
@RequiredArgsConstructor
public class Users {

    @Id
    @Column("id")
    private UUID id;

    @NotNull
    @Column("name")
    private String name;

    @NotNull
    @Column("email")
    private String email;
}
