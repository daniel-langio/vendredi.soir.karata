package vendredi.soir.karata.endpoint.rest.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import vendredi.soir.karata.endpoint.rest.exception.*;
import vendredi.soir.karata.endpoint.rest.model.Error;

@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * The web-ui SPA (static resources) uses real per-page paths (e.g. /table/{gameId}) via Flutter's
   * path URL strategy - refreshing or opening one of those paths directly hits this server first,
   * before any client-side routing can run, and Spring's static resource handler throws this
   * exception for anything that isn't an actual file. Forward those cases to index.html instead so
   * the SPA loads and its own router takes over; genuine API/health 404s are untouched. This has to
   * be declared here (rather than in a separate advice class) so Spring's per-class
   * most-specific-exception-wins resolution picks it over handleGeneric below - exception-handler
   * priority across *different* advice beans is resolved by bean iteration order, not specificity,
   * so a separate class could lose to the catch-all non-deterministically.
   */
  @ExceptionHandler(NoResourceFoundException.class)
  public void handleMissingStaticResource(
      NoResourceFoundException e, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    String path = request.getRequestURI();
    boolean isApiOrHealthPath =
        path.startsWith("/poker") || path.equals("/ping") || path.startsWith("/health");

    if (isApiOrHealthPath) {
      response.sendError(HttpStatus.NOT_FOUND.value(), e.getMessage());
      return;
    }

    // Deployments that don't bundle the web-ui SPA (e.g. this API alone on Render) have no
    // index.html at all - forwarding there unconditionally would re-trigger this same handler
    // forever (a missing index.html "not found" forwards to index.html, which is still missing,
    // forwards again, ...) until the request thread dies with a StackOverflowError. Only forward
    // when index.html actually exists, and never for a request that's already for it.
    boolean indexHtmlExists =
        !path.equals("/index.html")
            && request.getServletContext().getResource("/index.html") != null;

    if (!indexHtmlExists) {
      response.sendError(HttpStatus.NOT_FOUND.value(), e.getMessage());
      return;
    }
    request.getRequestDispatcher("/index.html").forward(request, response);
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<Error> handleBadRequest(BadRequestException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new Error("BAD_REQUEST", e.getMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Error> handleIllegalArgument(IllegalArgumentException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new Error("BAD_REQUEST", e.getMessage()));
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<Error> handleUnauthorized(UnauthorizedException e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(new Error("UNAUTHORIZED", e.getMessage()));
  }

  @ExceptionHandler(SecurityException.class)
  public ResponseEntity<Error> handleSecurity(SecurityException e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(new Error("UNAUTHORIZED", e.getMessage()));
  }

  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<Error> handleForbidden(ForbiddenException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new Error("FORBIDDEN", e.getMessage()));
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<Error> handleNotFound(NotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Error("NOT_FOUND", e.getMessage()));
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<Error> handleConflict(ConflictException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(new Error("CONFLICT", e.getMessage()));
  }

  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<Error> handleNoSuchElement(NoSuchElementException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Error("NOT_FOUND", e.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Error> handleGeneric(Exception e) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            new Error(
                "INTERNAL_SERVER_ERROR",
                e.getMessage() != null ? e.getMessage() : "An unexpected error occurred"));
  }
}
