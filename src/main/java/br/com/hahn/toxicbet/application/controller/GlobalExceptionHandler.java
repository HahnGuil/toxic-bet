package br.com.hahn.toxicbet.application.controller;

import br.com.hahn.toxicbet.domain.exception.*;
import br.com.hahn.toxicbet.domain.model.enums.ErrorMessages;
import br.com.hahn.toxicbet.model.ErrorResponseDTO;
import br.com.hahn.toxicbet.util.DateTimeConverter;
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

/**
 * Global exception handler for the application.
 * Uses Spring's ControllerAdvice to capture and handle specific exceptions,
 * returning appropriate HTTP status codes and error details in the response.
 *
 * @author HahnGuil
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

//    401
    @ExceptionHandler(UserNotAuthorizedException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handlerUserNotAuthorizedException(UserNotAuthorizedException ex){
        var error = createErrorResponse(ex.getMessage(), getInstanteNow());
        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error));
    }

//    404
    @ExceptionHandler(TeamNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handlerTeamNotFoundException(TeamNotFoundException ex){
        var error = createErrorResponse(ex.getMessage(), getInstanteNow());
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handlerUserNotFoundException(UserNotFoundException ex){
        var error = createErrorResponse(ex.getMessage(), getInstanteNow());
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
    }

    @ExceptionHandler(MatchNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handlerMatchNotFoundException(MatchNotFoundException ex){
        var error = createErrorResponse(ex.getMessage(), getInstanteNow());
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
    }

//    409
    @ExceptionHandler(ConflictMatchTimeException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handlerConflictMatchTimeException(ConflictMatchTimeException ex){
        var error = createErrorResponse(ex.getMessage(), getInstanteNow());
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(error));
    }

//    422
    @ExceptionHandler(InvalidMatchTimeException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handlerInvalidMatchTimeException(InvalidMatchTimeException ex){
        var error = createErrorResponse(ex.getMessage(), getInstanteNow());
        return Mono.just(ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error));
    }

    @ExceptionHandler(MatchNotOpenForBettingException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handlerMatchNotOpenForBettingException(MatchNotOpenForBettingException ex){
        var error = createErrorResponse(ex.getMessage(), getInstanteNow());
        return Mono.just(ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error));
    }

    @ExceptionHandler(InvalidTeamException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handlerInvalidTeamException(InvalidTeamException ex){
        var error = createErrorResponse(ex.getMessage(), getInstanteNow());
        return Mono.just(ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handlerWebExchangeBindException(){
        var message = ErrorMessages.GENERIC_INVALID_FORMAT.getMessage();
        var error = createErrorResponse(message, getInstanteNow());
        return Mono.just(ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error));
    }

    @ExceptionHandler(InvalidMatchStateException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handlerInvalidMatchStateException(InvalidMatchStateException ex){
        var error = createErrorResponse(ex.getMessage(), getInstanteNow());
        return Mono.just(ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error));
    }

//    500
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handlerGenericException(Exception ex){
        log.error("GlobalHandler: A generic error was triggered. This is the trace: {}, at: {}", ex.getMessage(), DateTimeConverter.formatInstantNow());
        var error = createErrorResponse("Internal server error. Please try again later.", getInstanteNow());
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
    }

//    Util
    /**
     * Creates an error response object with the provided message and timestamp.
     *
     * @author HahnGuil
     * @param message The error message to include in the response.
     * @param instantNow The timestamp of when the error occurred.
     * @return An {@link ErrorResponseDTO} containing the error details.
     */
    private ErrorResponseDTO createErrorResponse(String message, Instant instantNow){
        var error = new ErrorResponseDTO();
        error.setMessage(message);
        error.setTimestamp(OffsetDateTime.ofInstant(instantNow, ZoneOffset.UTC));
        return error;
    }

    /**
     * Retrieves the current timestamp as an {@link Instant}.
     *
     * @author HahnGuil
     * @return The current timestamp.
     */
    private Instant getInstanteNow(){
        return Instant.now();
    }
}