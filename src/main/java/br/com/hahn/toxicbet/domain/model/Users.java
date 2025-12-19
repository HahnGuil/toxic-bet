package br.com.hahn.toxicbet.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@Table("users")
@AllArgsConstructor
@NoArgsConstructor
public class Users {

    @Id
    private UUID id;
    private String name;
    private String email;
}
