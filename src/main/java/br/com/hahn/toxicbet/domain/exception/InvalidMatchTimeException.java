package br.com.hahn.toxicbet.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InvalidMatchTimeException extends RuntimeException {
    public InvalidMatchTimeException(String message) {
        super(message);
    }
}
