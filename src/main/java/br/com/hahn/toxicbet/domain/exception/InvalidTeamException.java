package br.com.hahn.toxicbet.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InvalidTeamException extends RuntimeException {
    public InvalidTeamException(String message) {
        super(message);
    }
}
