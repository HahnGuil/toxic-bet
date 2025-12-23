package br.com.hahn.toxicbet.domain.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@Getter
@ToString
public enum SuccessMessages {

    REGISTER_USER("User successfully registered in the application."),
    REGISTER_MATCH("Match successfully registered");

    private final String message;
}
