package gsrs.module.substance.controllers;

import gsrs.cache.GsrsCache;
import gsrs.events.BeginReindexEvent;
import gsrs.events.IncrementReindexEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ReindexStatusEventListener {

    @Autowired
    private GsrsCache cache;

    @EventListener
    public void setTotal(BeginReindexEvent event){
        ReIndexController.ReindexStatus  status = (ReIndexController.ReindexStatus ) cache.getRaw(event.getId().toString());
        if(status !=null){
            status.setTotal(event.getNumberOfExpectedRecord());
        }
    }
    @EventListener
    public void incrementCount(IncrementReindexEvent event){
        ReIndexController.ReindexStatus  status = (ReIndexController.ReindexStatus ) cache.getRaw(event.getId().toString());
        if(status !=null){
            status.incrementCount();
            status.getListener().message(status.generateMessage());
        }
    }
}
