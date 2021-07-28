package gsrs.module.substance;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncats.common.util.CachedSupplier;
import gov.nih.ncats.molwitch.renderer.RendererOptions;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
@Data
public class RendererOptionsConfig {
    @Value("classpath:renderer.json")
    private Resource renderOptionsJson;

    private CachedSupplier<RendererOptions> rendererSupplier = CachedSupplier.of(()->{
        if(renderOptionsJson !=null) {
            ObjectMapper mapper = new ObjectMapper();
            try(InputStream in = renderOptionsJson.getInputStream()) {
                return mapper.readValue(in, RendererOptions.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new RendererOptions();
    });

    public RendererOptions getDefaultRendererOptions(){
        return rendererSupplier.get();
    }

    public RendererOptions getRendererOptionsByName(String name){
        if(name !=null) {
            if ("INN".equalsIgnoreCase(name)) {
                return RendererOptions.createINNLike();
            }
            if ("USP".equalsIgnoreCase(name)) {
                return RendererOptions.createUSPLike();
            }
            String lowerName = name.toLowerCase();
            if (lowerName.contains("ball") && lowerName.contains("stick")) {
                return RendererOptions.createBallAndStick();
            }
        }
        return rendererSupplier.get();
    }
}
