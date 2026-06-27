package vendredi.soir.karata.conf;

import org.springframework.test.context.DynamicPropertyRegistry;
import vendredi.soir.karata.PojaGenerated;

@PojaGenerated
public class EmailConf {

  void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("aws.ses.source", () -> "dummy-ses-source");
  }
}
