package gsrs.module.substance;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.ncats.common.util.CachedSupplier;
import gov.nih.ncats.molwitch.renderer.RendererOptions;
import lombok.Builder;
import lombok.Data;

@Configuration
@Data
public class RendererOptionsConfig {


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
        
        
        public FullRenderOptions copy() {
            return FullRenderOptions.builder().options(options.copy()).showShadow(showShadow).build();
        }
    }
    
    private CachedSupplier<FullRenderOptions> rendererSupplier = CachedSupplier.of(()->{
        if(legacyStyle !=null){
            FullRenderOptions opt = getRendererOptionsByName(legacyStyle);
            
            if(opt !=null){
                return opt;
            }
        }
        if(style !=null){
            FullRenderOptions opt = getRendererOptionsByName(style);
            if(opt !=null){
                return opt;
            }
        }
        if(rendererOptionsJsonFilePath !=null) {
            ObjectMapper mapper = new ObjectMapper();
            Resource optionsJson = new ClassPathResource(rendererOptionsJsonFilePath);
            if(optionsJson !=null) {
                try (InputStream in = optionsJson.getInputStream()) {
                    return FullRenderOptions.builder().options(mapper.readValue(in, RendererOptions.class)).build();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return FullRenderOptions.builder().options(new RendererOptions()).build();
    });

    public FullRenderOptions getDefaultRendererOptions(){
        return rendererSupplier.get();
    }

    private FullRenderOptions getRendererOptionsByName(String name){
        if(name !=null && !("CONF".equalsIgnoreCase(name))) {
            if ("INN".equalsIgnoreCase(name)) {
                return FullRenderOptions.builder().options(RendererOptions.createINNLike()).build();
            }
            if ("USP".equalsIgnoreCase(name)) {
                return FullRenderOptions.builder().options(RendererOptions.createUSPLike())
                        .showShadow(false)
                        .build();
            }
            if ("DEFAULT".equalsIgnoreCase(name)) {
                return FullRenderOptions.builder().options(RendererOptions.createDefault()).build();
            }
            String lowerName = name.toLowerCase();
            if (lowerName.contains("ball") && lowerName.contains("stick")) {
                return FullRenderOptions.builder().options(RendererOptions.createBallAndStick()).build();
            }
        }
        return null;
    }
}
