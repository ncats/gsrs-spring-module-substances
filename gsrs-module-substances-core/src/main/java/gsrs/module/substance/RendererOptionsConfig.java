package gsrs.module.substance;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncats.common.util.CachedSupplier;
import gov.nih.ncats.molwitch.renderer.RendererOptions;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

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

    private CachedSupplier<RendererOptions> rendererSupplier = CachedSupplier.of(()->{
        if(legacyStyle !=null){
            RendererOptions opt = getRendererOptionsByName(legacyStyle);
            if(opt !=null){
                return opt;
            }
        }
        if(style !=null){
            RendererOptions opt = getRendererOptionsByName(style);
            if(opt !=null){
                return opt;
            }
        }
        if(rendererOptionsJsonFilePath !=null) {
            ObjectMapper mapper = new ObjectMapper();
            Resource optionsJson = new ClassPathResource(rendererOptionsJsonFilePath);
            if(optionsJson !=null) {
                try (InputStream in = optionsJson.getInputStream()) {
                    return mapper.readValue(in, RendererOptions.class);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return new RendererOptions();
    });

    public RendererOptions getDefaultRendererOptions(){
        return rendererSupplier.get();
    }

    private RendererOptions getRendererOptionsByName(String name){
        if(name !=null && !("CONF".equalsIgnoreCase(name))) {
            if ("INN".equalsIgnoreCase(name)) {
                return RendererOptions.createINNLike();
            }
            if ("USP".equalsIgnoreCase(name)) {
                return RendererOptions.createUSPLike();
            }
            if ("DEFAULT".equalsIgnoreCase(name)) {
                return RendererOptions.createDefault();
            }
            String lowerName = name.toLowerCase();
            if (lowerName.contains("ball") && lowerName.contains("stick")) {
                return RendererOptions.createBallAndStick();
            }
        }
        return null;
    }
}
