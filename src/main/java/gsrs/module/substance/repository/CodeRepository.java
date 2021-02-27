package gsrs.module.substance.repository;

import gsrs.repository.GsrsVersionedRepository;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Substance;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.stream.Stream;

@Repository
public interface CodeRepository extends GsrsVersionedRepository<Code, UUID> {

    Stream<Code> findCodesByCodeSystemAndCodeLike(String codesystem, String codeLike);
}
