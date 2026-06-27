package vendredi.soir.karata.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vendredi.soir.karata.PojaGenerated;
import vendredi.soir.karata.repository.model.DummyUuid;

@PojaGenerated
@Repository
public interface DummyUuidRepository extends JpaRepository<DummyUuid, String> {
  @Override
  List<DummyUuid> findAllById(Iterable<String> ids);
}
