package vendredi.soir.karata.endpoint.rest.controller;

import static org.mockito.Mockito.*;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URL;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
  private final NoResourceFoundException exception =
      new NoResourceFoundException(org.springframework.http.HttpMethod.GET, "/whatever");

  @Test
  void forwards_to_index_html_when_it_exists_and_the_request_is_for_something_else()
      throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    ServletContext context = mock(ServletContext.class);
    RequestDispatcher dispatcher = mock(RequestDispatcher.class);

    when(request.getRequestURI()).thenReturn("/table/some-game-id");
    when(request.getServletContext()).thenReturn(context);
    when(context.getResource("/index.html")).thenReturn(new URL("file:/index.html"));
    when(request.getRequestDispatcher("/index.html")).thenReturn(dispatcher);

    handler.handleMissingStaticResource(exception, request, response);

    verify(dispatcher).forward(request, response);
    verify(response, never()).sendError(anyInt(), anyString());
  }

  @Test
  void returns_a_plain_404_instead_of_forwarding_when_index_html_does_not_exist() throws Exception {
    // The regression this guards against: a deployment with no web-ui bundled (so index.html
    // genuinely doesn't exist) would otherwise forward to index.html, fail to find it again,
    // forward again, forever - a real StackOverflowError seen on a live deployment.
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    ServletContext context = mock(ServletContext.class);

    when(request.getRequestURI()).thenReturn("/some/spa/route");
    when(request.getServletContext()).thenReturn(context);
    when(context.getResource("/index.html")).thenReturn(null);

    handler.handleMissingStaticResource(exception, request, response);

    verify(response).sendError(eq(404), anyString());
    verify(request, never()).getRequestDispatcher(anyString());
  }

  @Test
  void never_forwards_a_request_that_is_already_for_index_html_even_if_it_exists()
      throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    ServletContext context = mock(ServletContext.class);

    when(request.getRequestURI()).thenReturn("/index.html");
    when(request.getServletContext()).thenReturn(context);
    when(context.getResource("/index.html")).thenReturn(new URL("file:/index.html"));

    handler.handleMissingStaticResource(exception, request, response);

    verify(response).sendError(eq(404), anyString());
    verify(request, never()).getRequestDispatcher(anyString());
  }

  @Test
  void api_and_health_paths_always_get_a_plain_404_without_touching_the_servlet_context()
      throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    when(request.getRequestURI()).thenReturn("/poker/games/some-id");

    handler.handleMissingStaticResource(exception, request, response);

    verify(response).sendError(eq(404), anyString());
    verify(request, never()).getServletContext();
  }
}
