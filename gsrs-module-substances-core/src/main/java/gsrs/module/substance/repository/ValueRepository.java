package gsrs.module.substance.repository;

import gsrs.repository.GsrsVersionedRepository;
import ix.core.models.Value;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ValueRepository extends GsrsVersionedRepository<Value, Long> {
}
