package gsrs.module.substance.tasks;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

import gsrs.controller.AbstractLegacyTextSearchGsrsEntityController;
import gsrs.controller.hateoas.GsrsEntityToControllerMapper;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.springUtils.StaticContextAccessor;
import ix.utils.Util;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DatabaseIndexSyncTaskInitializer extends ScheduledTaskInitializer{

	private String entityString;
	
	@JsonProperty("entityString")
	public void setEntityString(String entities) {
		this.entityString = entities;
	}
	
	
	private final Set<String> supportedEntities = Util.toSet("Code","ControlledVocabulary","Name", "Reference","Substance");
	
	@Override
	public void run(SchedulerPlugin.JobStats stats, SchedulerPlugin.TaskListener l) {
		if(entityString.length()==0) {
			log.info("No entities defined for the database and index sync scheduler.");
			return;
		}
		
		Set<String> syncEntities = Stream.of(entityString.split(",")).map(entity->entity.trim()).collect(Collectors.toSet());
		syncEntities.retainAll(supportedEntities);
		if(syncEntities.size()==0) {
			log.info("No allowed entities defined for the database and index sync scheduler.");
			return;
		}
				
		for(String entity: syncEntities) {
						
			String entityClassName = generateEntityClassName(entity);
			log.info("Entity class: " + entityClassName);
			
			if(entityClassName.isEmpty()) {
				log.warn("Illegal entity class: " + entity + " in database and index sync scheduler.");
				continue;
			}
			
			Class<?> entityClass;				
			try {
				entityClass = Class.forName(entityClassName);
			} catch (ClassNotFoundException e) {
				log.warn("Entity class is not found: " + entityClassName);
				continue;
			}
			
			GsrsEntityToControllerMapper mapper = StaticContextAccessor.getBean(GsrsEntityToControllerMapper.class);
	        if(mapper ==null){
	            continue;
	        }
	        Optional<Class> controllerOpt = mapper.getControllerFor(entityClass);
	        if(!controllerOpt.isPresent()) {
	        	continue;
	        }
	      		
			AbstractLegacyTextSearchGsrsEntityController searchController = (AbstractLegacyTextSearchGsrsEntityController) StaticContextAccessor.getBean(controllerOpt.get());
			try {
				searchController.syncIndexesWithDatabase();
			} catch (JsonProcessingException e) {
				log.error("Error in database and index sync scheduler: " + entityClassName);
				e.printStackTrace();
				continue;
			}
		}
	}
	
	private String generateEntityClassName(String entity) {
		return "ix.ginas.models.v1." + entity;		
	}

	@Override
	public String getDescription() {
		return "Reindex entities in backup tables that are not in indexes";
	}

}
