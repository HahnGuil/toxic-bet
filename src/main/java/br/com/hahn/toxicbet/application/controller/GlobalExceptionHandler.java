package br.com.hahn.toxicbet.application.controller;

import br.com.hahn.toxicbet.domain.exception.*;
import br.com.hahn.toxicbet.domain.model.enums.ErrorMessages;
import br.com.hahn.toxicbet.model.ErrorResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
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

    @ExceptionHandler(InvalidTeamException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handlerInvalidTeamException(InvalidTeamException ex){
        var error = createErrorResponse(ex.getMessage(), getInstanteNow());
        return Mono.just(ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error));
    }

    @ExceptionHandler(ConflitMathcTimeException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handlerConflictMatchTimeException(ConflitMathcTimeException ex){
        var error = createErrorResponse(ex.getMessage(), getInstanteNow());
        return Mono.just(ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error));
    }

    @ExceptionHandler(UserNotAuthorizedException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handlerUserNotAuthorizedException(UserNotAuthorizedException ex){
        var error = createErrorResponse(ex.getMessage(), getInstanteNow());
        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handlerWebExchangeBindException(){
        var message = ErrorMessages.GENERIC_INVALID_FORMAT.getMessage();
        var error = createErrorResponse(message, getInstanteNow());
        return Mono.just(ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handlerGenericException(Exception ex){
        log.error("GlobalHandler: A generic error was triggered. This is the trace: {}", ex.getMessage());
        var error = createErrorResponse("Internal server error. Please try again later.", getInstanteNow());
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
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