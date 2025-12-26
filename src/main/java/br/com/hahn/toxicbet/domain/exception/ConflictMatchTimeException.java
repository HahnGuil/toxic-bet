package br.com.hahn.toxicbet.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictMatchTimeException extends RuntimeException {
    public ConflictMatchTimeException(String message) {
        super(message);
    }
}
