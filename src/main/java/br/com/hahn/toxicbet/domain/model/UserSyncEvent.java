package br.com.hahn.toxicbet.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
@ToString
public class UserSyncEvent {
    private String uuid;
    private String applicationCode;

}
