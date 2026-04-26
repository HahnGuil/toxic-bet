package br.com.hahn.toxicbet.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UserUpdateEvent {
    private String email;
    private String applicationName;
}
