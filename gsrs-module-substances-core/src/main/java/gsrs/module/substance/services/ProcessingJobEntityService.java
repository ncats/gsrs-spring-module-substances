package gsrs.module.substance.services;

import gsrs.DefaultDataSourceConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Qualifier;
import tools.jackson.databind.JsonNode;
import gsrs.controller.IdHelpers;
import gsrs.events.AbstractEntityCreatedEvent;
import gsrs.events.AbstractEntityUpdatedEvent;
import gsrs.module.substance.repository.KeywordRepository;
import gsrs.module.substance.repository.ProcessingJobRepository;
import gsrs.service.AbstractGsrsEntityService;
import ix.core.models.Keyword;
import ix.core.models.ProcessingJob;
import ix.core.models.ProcessingJobUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
@Service
@ExposesResourceFor(ProcessingJob.class)
public class ProcessingJobEntityService extends AbstractGsrsEntityService<ProcessingJob, Long> {
    public static final String  CONTEXT = "jobs";

    @Autowired
    private ProcessingJobRepository processingJobRepository;

    @Autowired
    private KeywordRepository keywordRepository;

    @PersistenceContext(unitName = DefaultDataSourceConfig.NAME_ENTITY_MANAGER)
    private EntityManager entityManager;

    @Autowired
    @Qualifier("legacyJsonMapper")
    private JsonMapper mapper;

    public ProcessingJobEntityService() {
        super(CONTEXT,  IdHelpers.NUMBER, "gsrs_exchange", "jobs.created", "jobs.updated");

    }

    @Override
    protected ProcessingJob fromNewJson(JsonNode json){
        return mapper.convertValue(json, getEntityClass());
    }

    @Override
    protected ProcessingJob fromUpdatedJson(JsonNode json) {
        return mapper.convertValue(json, getEntityClass());
    }

    @Override
    protected JsonNode toJson(ProcessingJob processingJob) throws IOException {
        return mapper.valueToTree(processingJob);
    }

    @Override
    protected ProcessingJob create(ProcessingJob processingJob) {
        return processingJobRepository.saveAndFlush(processingJob);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProcessingJob> get(Long id) {
        return processingJobRepository.findById(id).map(this::initializeJob);
    }

    private ProcessingJob initializeJob(ProcessingJob job) {
        // The upload/status response is serialized after the service transaction closes.
        job.keys.size();
        return job;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<ProcessingJob> getLoadStatus(String jobKey) {
        Optional<ProcessingJob> jobByKey = flexLookup(jobKey);
        if (jobByKey.isEmpty() && jobKey.matches("[0-9]{1,18}")) {
            jobByKey = processingJobRepository.findById(Long.parseLong(jobKey));
        }
        return jobByKey.map(job -> {
            // Polling must not combine a cached job entity with live in-memory statistics.
            entityManager.refresh(job);
            return initializeJob(job);
        });
    }

    @Override
    public Long parseIdFromString(String idAsString) {
        return Long.parseLong(idAsString);
    }

    @Override
    public Long getIdFrom(ProcessingJob entity) {
        return entity.id;
    }

    @Override
    protected ProcessingJob update(ProcessingJob processingJob) {
        return processingJobRepository.save(processingJob);
    }

    @Override
    protected AbstractEntityUpdatedEvent<ProcessingJob> newUpdateEvent(ProcessingJob updatedEntity) {
        return null;
    }

    @Override
    protected AbstractEntityCreatedEvent<ProcessingJob> newCreationEvent(ProcessingJob createdEntity) {
        return null;
    }

    @Override
    public Optional<ProcessingJob> flexLookup(String someKindOfId) {
        //check for job keyword?
        List<Keyword> matches = keywordRepository.findAll(Example.of(new Keyword(ProcessingJobUtils.LEGACY_PLUGIN_LABEL_KEY, someKindOfId)));
        if(!matches.isEmpty()){

            List<ProcessingJob> jobs = processingJobRepository.findByKeysIn(matches.size() ==1? matches : Collections.singletonList(matches.get(0)));
            if(!jobs.isEmpty()){
                return Optional.of(jobs.get(0));
            }
        }
        return Optional.empty();
    }

    @Override
    public long count() {
        return processingJobRepository.count();
    }

    @Override
    public void delete(Long id) {
        processingJobRepository.deleteById(id);
    }

    @Override
    public Class<ProcessingJob> getEntityClass() {
        return ProcessingJob.class;
    }

    @Override
    public Page page(Pageable pageable) {
        return processingJobRepository.findAll(pageable);
    }

	@Override
	public List<Long> getIDs() {		
		return processingJobRepository.getAllIDs();
	}
}
