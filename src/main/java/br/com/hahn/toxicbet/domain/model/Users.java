package br.com.hahn.toxicbet.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("users")
@AllArgsConstructor
@NoArgsConstructor
public class Users {

    @Id
    private Long id;
    private String name;
    private String email;
}
