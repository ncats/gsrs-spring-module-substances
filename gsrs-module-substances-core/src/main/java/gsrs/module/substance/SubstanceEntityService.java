package gsrs.module.substance;

import gsrs.service.GsrsEntityService;
import ix.ginas.models.v1.Substance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface SubstanceEntityService extends GsrsEntityService<Substance, UUID> {
    @Override
    Class<Substance> getEntityClass();

    UUID parseIdFromString(String idAsString);

    @Override
    Page page(Pageable pageable);

    @Override
    void delete(UUID id);

    UUID getIdFrom(Substance entity);

    @Override
    long count();

    Optional<Substance> get(UUID id);

    Optional<Substance> flexLookup(String someKindOfId);
}
