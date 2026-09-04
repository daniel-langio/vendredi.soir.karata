package vendredi.soir.karata.endpoint;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import vendredi.soir.karata.PojaGenerated;

@PojaGenerated
@Configuration
public class EndpointConf {
  @Bean
  @Primary
  public ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.configure(WRITE_DATES_AS_TIMESTAMPS, false);
    // Some persisted domain actions (e.g. Showdown) have no fields at all.
    objectMapper.configure(FAIL_ON_EMPTY_BEANS, false);
    objectMapper.findAndRegisterModules();
    return objectMapper;
  }
}
