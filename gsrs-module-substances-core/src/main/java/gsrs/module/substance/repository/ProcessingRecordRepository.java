package gsrs.module.substance.repository;

import ix.core.models.ProcessingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessingRecordRepository extends JpaRepository<ProcessingRecord, Long> {

}
