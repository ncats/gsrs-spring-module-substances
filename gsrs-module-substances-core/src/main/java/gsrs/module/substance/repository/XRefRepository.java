package gsrs.module.substance.repository;

import ix.core.models.XRef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface XRefRepository extends JpaRepository<XRef, Long> {
}
