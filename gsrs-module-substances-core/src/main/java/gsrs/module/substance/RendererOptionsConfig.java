package gsrs.module.substance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import gov.nih.ncats.common.util.CachedSupplier;
import gov.nih.ncats.molwitch.renderer.RendererOptions;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Resolves the render options used when drawing chemical structures.
 *
 * <p>The options are looked up, in order, from {@code gsrs.renderers.selected} (the GSRS 2.x
 * setting), from {@code substance.renderer.style} and finally from the built in defaults.  Named
 * renderers, including any low level {@code RendererOptions} settings, are declared under
 * {@code gsrs.renderers.list} in the HOCON configuration.
 *
 * <p>This used to additionally read a json file named by {@code substance.renderer.configPath};
 * that external file is no longer supported and its contents are now declared directly in the
 * configuration instead.
 */
@Configuration
@Data
@Slf4j
public class RendererOptionsConfig {


	@Autowired
	GSRSRendererConfiguration rendererConf;
	
	
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
        return FullRenderOptions.builder().options(new RendererOptions()).build();
    });

    public FullRenderOptions getDefaultRendererOptions(){
        return rendererSupplier.get();
    }

}
