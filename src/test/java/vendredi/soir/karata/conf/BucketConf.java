package vendredi.soir.karata.conf;

import org.springframework.test.context.DynamicPropertyRegistry;
import vendredi.soir.karata.PojaGenerated;

@PojaGenerated
public class BucketConf {

  void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("aws.s3.bucket", () -> "dummy-bucket");
  }
}
