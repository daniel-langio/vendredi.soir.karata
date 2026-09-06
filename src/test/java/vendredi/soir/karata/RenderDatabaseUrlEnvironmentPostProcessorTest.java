package vendredi.soir.karata;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class RenderDatabaseUrlEnvironmentPostProcessorTest {

  private final RenderDatabaseUrlEnvironmentPostProcessor processor =
      new RenderDatabaseUrlEnvironmentPostProcessor();

  @Test
  void rewrites_a_render_style_postgres_url_into_jdbc_form_with_separate_credentials() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty(
        "DATABASE_URL", "postgres://alice:s3cret@db.render.com:5432/karata?sslmode=require");

    processor.postProcessEnvironment(environment, null);

    assertEquals(
        "jdbc:postgresql://db.render.com:5432/karata?sslmode=require",
        environment.getProperty("spring.datasource.url"));
    assertEquals("alice", environment.getProperty("spring.datasource.username"));
    assertEquals("s3cret", environment.getProperty("spring.datasource.password"));
  }

  @Test
  void leaves_an_already_jdbc_url_untouched() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("DATABASE_URL", "jdbc:postgresql://localhost:5442/postgres");

    processor.postProcessEnvironment(environment, null);

    assertNull(environment.getProperty("spring.datasource.url"));
    assertNull(environment.getProperty("spring.datasource.username"));
  }

  @Test
  void does_nothing_when_database_url_is_unset() {
    MockEnvironment environment = new MockEnvironment();

    processor.postProcessEnvironment(environment, null);

    assertNull(environment.getProperty("spring.datasource.url"));
  }

  @Test
  void handles_a_url_with_no_query_string() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("DATABASE_URL", "postgres://bob:pw@db.render.com:5432/karata");

    processor.postProcessEnvironment(environment, null);

    assertEquals(
        "jdbc:postgresql://db.render.com:5432/karata",
        environment.getProperty("spring.datasource.url"));
  }
}
