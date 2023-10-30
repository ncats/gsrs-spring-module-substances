package gsrs.module.substance;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;

import gov.nih.ncats.molvec.internal.util.CachedSupplier;
import gov.nih.ncats.molwitch.renderer.RendererOptions;
import gsrs.module.substance.RendererOptionsConfig.FullRenderOptions;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Configuration
@ConfigurationProperties("gsrs.renderers")
@Data
@Slf4j
public class GSRSRendererConfiguration {
	
	private String selected;
	
	private LinkedHashMap<Integer,Map> list;
	
	@JsonIgnore
	@Getter(value=AccessLevel.NONE)
	@Setter(value=AccessLevel.NONE)
	private CachedSupplier<Map<String,FullRenderOptions>> _fullRenderer = CachedSupplier.of(()->{
		Map<String, FullRenderOptions> renderMap = new HashMap<>();
		
		int i=0;
		if(list!=null) {
			for(Map m:list.values()) {
				String name = m.getOrDefault("name", "").toString();
				FullRenderOptions base = FullRenderOptions.builder().build();
				if(name==null || name.length()==0) {
					log.error("Render settings must have names, gsrs.renderers.list[%i] does not have a name. That render config will be skipped.", i);
					continue;
				}
				
				Map<String, Object> renderer = (Map<String, Object>) m.get("renderer");
				if(renderer==null || !(renderer instanceof Map)) {
					log.error("Render settings must have 'renderer' setting, gsrs.renderers.list[%i].renderer is not expected format.", i);
					continue;
				}
				
				if(renderer.get("preset")!=null) {
					String preset=renderer.get("preset").toString();
					base=renderMap.getOrDefault(preset, getFullRendererOptionsByName(preset).orElse(null));
					if(base==null) {
						log.warn("Render settings preset '%s' not found as a valid preset. The default render config will be used instead.", preset);					
						base = FullRenderOptions.builder().build();
					}
				}
				Map<String,Object> options = (Map<String,Object>) renderer.get("options");
				if(options!=null) {
					
				}
				
				
				
				renderMap.put(name,base);
				i++;
			}
		}
		return renderMap;
	});

	

    private Optional<FullRenderOptions> getDefaultRendererOptionsByName(String name){
    	if(name !=null) {
            if ("INN".equalsIgnoreCase(name)) {
                return Optional.ofNullable(FullRenderOptions.builder().options(RendererOptions.createINNLike()).build());
            }
            if ("USP".equalsIgnoreCase(name)) {
                return Optional.ofNullable(FullRenderOptions.builder().options(RendererOptions.createUSPLike())
                        .showShadow(false)
                        .build());
            }
            if ("DEFAULT".equalsIgnoreCase(name)) {
                return Optional.ofNullable(FullRenderOptions.builder().options(RendererOptions.createDefault()).build());
            }
            String lowerName = name.toLowerCase();
            if (lowerName.contains("ball") && lowerName.contains("stick")) {
                return Optional.ofNullable(FullRenderOptions.builder().options(RendererOptions.createBallAndStick()).build());
            }
        }
        return Optional.empty();
    }
    
    public Optional<FullRenderOptions> getFullRendererOptionsByName(String name){
    	//TODO: not sure the CONF part is needed anymore
        if(name !=null && !("CONF".equalsIgnoreCase(name))) {
           return Optional.ofNullable(getDefaultRendererOptionsByName(name)
        		   .orElseGet(()->_fullRenderer.get().get(name)));
        }
        return Optional.empty();
    } 
	
}
