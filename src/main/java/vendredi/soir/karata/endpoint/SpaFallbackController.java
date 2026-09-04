package vendredi.soir.karata.endpoint;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * The web-ui SPA (served from src/main/resources/static/) uses real per-page paths (e.g.
 * /table/{gameId}) via Flutter's path URL strategy - refreshing or opening one of those paths
 * directly hits this server first, before any client-side routing can run, and Spring's static
 * resource handler 404s on anything that isn't an actual file. Forward those cases to index.html
 * instead so the SPA loads and its own router takes over. The /poker/** API and health endpoints
 * keep returning a normal 404 - only genuinely unmapped, non-API paths get the SPA shell.
 */
@Controller
public class SpaFallbackController implements ErrorController {

  @RequestMapping("/error")
  public String handleError(HttpServletRequest request) {
    Object statusAttr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
    int status = statusAttr != null ? Integer.parseInt(statusAttr.toString()) : 500;
    Object pathAttr = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
    String path = pathAttr != null ? pathAttr.toString() : "";

    boolean isApiOrHealthPath =
        path.startsWith("/poker") || path.equals("/ping") || path.startsWith("/health");

    if (status == 404 && !isApiOrHealthPath) {
      return "forward:/index.html";
    }
    return "error";
  }
}
