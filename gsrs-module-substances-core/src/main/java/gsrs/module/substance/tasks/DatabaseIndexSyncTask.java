package gsrs.module.substance.tasks;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


import ix.utils.Util;
import jdk.internal.joptsimple.internal.Strings;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(name = "entity.database.index.sync.scheduler.enabled", havingValue = "true")
public class DatabaseIndexSyncTask {
		
	@Value("${server.port}")
	private int serverPort;
	
	@Value("${server.url.prefix}")
	private String urlPrefix;
	
	@Value("${database.index.sync.scheduler.entities}")
	private String entityString;
	
	@Value("${database.index.sync.scheduler.cron}")
	private String cron;
	
	private final Set<String> allowedEntities = Util.toSet("substances", "codes", "names");	
			
	@Scheduled(cron= "${database.index.sync.scheduler.cron}")	
	public void runSyncTask() {
		
		String [] syncEntities = entityString.split(",");
		if(syncEntities.length==0)
			return;
		
		for(String entity: syncEntities) {
			if(!allowedEntities.contains(entity.toLowerCase()))
				continue;
			String synUrl = generateUrl(urlPrefix, entity);
			System.out.println(synUrl);
			RestTemplate restTemplate = new RestTemplate();
	    	ResponseEntity<Object> response  =  restTemplate.postForEntity(synUrl,null,Object.class);
	    	if(!response.getStatusCode().is2xxSuccessful()) {
	    		log.warn("Sync job for " + entity+ " failed.");
	    	}
		}	
	}
	
	private String generateUrl(String urlPrefix, String entity) {
		if(urlPrefix.isEmpty() || entity.isEmpty())
			return Strings.EMPTY;
		if(urlPrefix.endsWith("/"))
			return urlPrefix + entity + "/@databaseIndexSync";
		else 
			return urlPrefix + "/" + entity + "/@databaseIndexSync";
	}
}
