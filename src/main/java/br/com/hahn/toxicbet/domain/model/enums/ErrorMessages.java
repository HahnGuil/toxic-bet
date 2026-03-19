package br.com.hahn.toxicbet.domain.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@Getter
@ToString
public enum ErrorMessages {

    INVALID_MATCH_TIME("The date of the match has already passed, please provide a future date."),
    HOME_TEAM_NOT_FOUND("Home Team not found, or not registered."),
    VISITING_TEAM_NOT_FOUND("Visiting Team not found, or not registered."),
    INVALID_DATE_MATCH_TIME("Invalid match date format. Match date must be DD/MM/YYYY HH:MM"),
    GENERIC_INVALID_FORMAT("There is an invalid format in the request; please check the fields entered and try again."),
    UNAUTHORIZED_MESSAGE("Unauthorized user. Please log in or verify the access credentials for this resource."),
    FORBIDDEN_OPERATION("Only administrators can perform this operation."),
    TEAM_MUST_BE_DIFFERENT("The home team and the visiting team cannot be the same."),
    CONFLICT_MATCH_TIME("One of the teams in the match already has a game scheduled for this date and time."),
    CAN_NOT_UPDATE_MATCH("Cannot update score for match not in progress"),
    MATCH_NOT_FOUND("Match not found for this id: "),
    USER_NOT_FOUND("User not found!"),
    CHAMPIONSHIP_NOT_FOUND("Championship not found"),
    MATCH_NOT_OPEN_TO_BETS("This match is not eligible for betting."),
    BETTING_POOL_NOT_FOUND_KEY("Not found Betting Pool for the given key");

    private final String message;
}
