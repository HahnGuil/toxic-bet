package br.com.hahn.toxicbet.domain.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@Getter
@ToString
public enum ErrorMessages {

    INVALID_MATCH_TIME("The departure date has already passed."),
    HOME_TEAM_NOT_FOUND("Home Team not found, or not registered."),
    VISITING_TEAM_NOT_FOUND("Visiting Team not found, or not registered."),
    INVALID_DATE_MATCH_TIME("Invalid match date format. Match date must be DD/MM/YYYY HH:MM"),
    GENERIC_INVALID_FORMAT("There is an invalid format in the request; please check the fields entered and try again."),
    UNAUTHORIZED_MESSAGE("Unauthorized user. Please log in or verify the access credentials for this resource.");

    private final String message;
}
