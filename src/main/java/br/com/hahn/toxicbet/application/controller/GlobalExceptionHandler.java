package br.com.hahn.toxicbet.application.controller;

import br.com.hahn.toxicbet.domain.exception.InvalidMatchTimeException;
import br.com.hahn.toxicbet.domain.exception.TeamNotFoundException;
import br.com.hahn.toxicbet.domain.model.enums.ErrorMessages;
import br.com.hahn.toxicbet.model.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidMatchTimeException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handlerInvalidMatchTimeException(InvalidMatchTimeException ex){
        var error = createErrorResponse(ex.getMessage(), getInstanteNow());
        return Mono.just(ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error));
    }

    @ExceptionHandler(TeamNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handlerTeamNotFoundException(TeamNotFoundException ex){
        var error = createErrorResponse(ex.getMessage(), getInstanteNow());
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handlerValidationFields(MethodArgumentNotValidException ex){
        var message = ErrorMessages.GENERIC_INVALID_FORMAT.getMessage();
        var error = createErrorResponse(message, Instant.now());
        return Mono.just(ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error));
    }

    private ErrorResponseDTO createErrorResponse(String message, Instant instantNow){
        var error = new ErrorResponseDTO();
        error.setMessage(message);
        error.setTimestamp(OffsetDateTime.ofInstant(instantNow, ZoneOffset.UTC));
        return error;
    }

    private Instant getInstanteNow(){
        return Instant.now();
    }
}