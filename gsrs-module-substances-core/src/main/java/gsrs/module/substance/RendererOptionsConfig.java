package gsrs.module.substance;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.ncats.common.util.CachedSupplier;
import gov.nih.ncats.molwitch.renderer.RendererOptions;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Data
@Slf4j
public class RendererOptionsConfig {


	@Autowired
	GSRSRendererConfiguration rendererConf;
	
	
    @Value("${substance.renderer.configPath:#{null}}")
    private String rendererOptionsJsonFilePath;
    @Value("${substance.renderer.style:#{null}}")
    private String style;
    //this was the way to specify it in GSRS 2.x.  if this is in the conf then we use it
    @Value(value ="${gsrs.renderers.selected:#{null}}" )
    private String legacyStyle;

    @Data
    @Builder
    public static class FullRenderOptions{
        private RendererOptions options;
        @Builder.Default
        private boolean showShadow=true;
        @Builder.Default
        private boolean addBorder=false;
        private String colorBg;
        private String colorBorder;
        
        
        public FullRenderOptions copy() {
            return toBuilder()
            		.build();
        } 
        public FullRenderOptionsBuilder toBuilder(){
        	return FullRenderOptions.builder()
            		.options(options.copy())
            		.showShadow(showShadow)
            		.addBorder(addBorder)
            		.colorBg(colorBg)
            		.colorBorder(colorBorder);
        }
    }
    
    private CachedSupplier<FullRenderOptions> rendererSupplier = CachedSupplier.of(()->{
        if(legacyStyle !=null){
            FullRenderOptions opt = rendererConf.getFullRendererOptionsByName(legacyStyle).orElse(null);
            
            if(opt !=null){
                return opt;
            }
        }
        if(style !=null){
        	FullRenderOptions opt = rendererConf.getFullRendererOptionsByName(style).orElse(null);
            if(opt !=null){
                return opt;
            }
        }
        if(rendererOptionsJsonFilePath !=null) {
            ObjectMapper mapper = new ObjectMapper();
            Resource optionsJson = new ClassPathResource(rendererOptionsJsonFilePath);
            if(optionsJson !=null) {
                try (InputStream in = optionsJson.getInputStream()) {
                	RendererOptions options= RendererOptions.createFromMap((Map<String, ?>) mapper.readValue(in, Map.class));
                	
                    return FullRenderOptions.builder().options(options).build();
                } catch (IOException e) {
                    log.error("Error deserializing renderer options");
                }
            }
        }
        return FullRenderOptions.builder().options(new RendererOptions()).build();
    });

    public FullRenderOptions getDefaultRendererOptions(){
        return rendererSupplier.get();
    }

}
