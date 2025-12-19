package br.com.hahn.toxicbet.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@Getter
@ToString
public enum SuccessMessages {

    REGISTER_USER("User successfully registered in the application.");

    private final String message;
}
