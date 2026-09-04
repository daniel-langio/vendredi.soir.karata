package vendredi.soir.karata.endpoint;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows the web client to call this API cross-origin. Note this only covers requests that reach
 * Spring's DispatcherServlet - when deployed behind an AWS Lambda Function URL, that layer enforces
 * its own separate CORS policy first and must be configured independently (this app has no access
 * to that infrastructure setting).
 */
@Configuration
public class WebCorsConfig implements WebMvcConfigurer {
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/**")
        .allowedOrigins("*")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*");
  }
}
