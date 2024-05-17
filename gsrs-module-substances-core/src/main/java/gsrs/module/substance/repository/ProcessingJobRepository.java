package gsrs.module.substance.repository;

import ix.core.models.Keyword;
import ix.core.models.ProcessingJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Repository
@Transactional
public interface ProcessingJobRepository extends JpaRepository<ProcessingJob, Long> {

   List<ProcessingJob> findByKeysIn(Collection<Keyword> keyword);
   
   @Query("select pj.id from ProcessingJob pj")
   List<Long> getAllIDs();
}
