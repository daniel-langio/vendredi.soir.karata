package vendredi.soir.karata.endpoint.rest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vendredi.soir.karata.endpoint.rest.model.Error;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public Error handleException(Exception e) {
    return new Error("INTERNAL_SERVER_ERROR", e.getMessage());
  }

  @ExceptionHandler(RuntimeException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Error handleRuntimeException(RuntimeException e) {
    return new Error("BAD_REQUEST", e.getMessage());
  }
}
