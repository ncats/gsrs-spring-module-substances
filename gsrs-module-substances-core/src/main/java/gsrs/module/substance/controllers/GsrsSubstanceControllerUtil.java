package gsrs.module.substance.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import gsrs.cache.GsrsCache;
import ix.core.controllers.EntityFactory;

import java.util.Optional;

public class GsrsSubstanceControllerUtil {

    public static <T> Optional<T> getTempObject(GsrsCache ixCache, String id, Class<T> objectClass) {
        String json = (String) ixCache.getRaw(id);

        if(json !=null){
            try {
                return Optional.of(EntityFactory.EntityMapper.INTERNAL_ENTITY_MAPPER().readValue(json, objectClass));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        return Optional.empty();
    }
}
