package gsrs.module.substance.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import gsrs.cache.GsrsCache;
import gsrs.controller.*;
import gsrs.module.substance.services.ReindexService;
import gsrs.scheduledTasks.SchedulerPlugin;
import ix.core.EntityMapperOptions;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.persistence.Id;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@ExposesResourceFor(ReIndexController.ReindexStatus.class)
@GsrsRestApiController(context = "reindex",  idHelper = IdHelpers.UUID)
public class ReIndexController {

    @Autowired
    private GsrsCache ixCache;

    @Autowired
    private ReindexService reindexService;

    @Autowired
    private GsrsControllerConfiguration gsrsControllerConfiguration;

    @Data
    @EntityMapperOptions()
    public static class ReindexStatus {
        @JsonProperty("status")
        private SchedulerPlugin.TaskListener listener;
        @Id
        private UUID uuid;

        private long total;

        private AtomicLong count= new AtomicLong(0);

        public void incrementCount(){
            count.incrementAndGet();
        }

        public String generateMessage(){
            return count.get() + " of "+ total;
        }
    }

    @GetGsrsRestApiMapping({"({ID})","/{ID}"})
    public Object get(@PathVariable("ID") UUID id, @RequestParam Map<String,String> queryParameters){
        ReIndexController.ReindexStatus status = (ReIndexController.ReindexStatus) ixCache.getRaw(id.toString());
        if(status ==null){
            return gsrsControllerConfiguration.handleNotFound(queryParameters);
        }

        return GsrsControllerUtil.enhanceWithView(status, queryParameters);
    }
}
