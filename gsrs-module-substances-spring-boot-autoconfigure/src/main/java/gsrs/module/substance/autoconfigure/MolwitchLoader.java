package gsrs.module.substance.autoconfigure;

import gov.nih.ncats.molwitch.Chemical;
import gov.nih.ncats.molwitch.ChemicalBuilder;
import gov.nih.ncats.molwitch.ImplUtil;
import gov.nih.ncats.molwitch.MolWitch;
import gov.nih.ncats.molwitch.inchi.Inchi;
import gov.nih.ncats.molwitch.spi.ChemicalImplFactory;
import ix.core.chem.StructureProcessorConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
public class MolwitchLoader implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private StructureProcessorConfiguration processorConfiguration;

    @Value("#{new Boolean('${gsrs.substances.molwitch.enabled:true}')}")
    private boolean enabled = true;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        if(enabled) {
            invokeMolwitch();
        }
    }

    private void invokeMolwitch() {
        Chemical build = null;
        try {
            build = ChemicalBuilder.createFromSmiles("O=C=O").build();

            String smiles = build.toSmiles();

            String inchi = Inchi.asStdInchi(build).getKey();

            Map<String,Object> params = processorConfiguration.getMolwitch();
            ChemicalImplFactory defFAC = ImplUtil.getChemicalImplFactory();
            log.trace("molwitch parameters");
            params.entrySet().forEach(e->log.trace("key: {} = {}", e.getKey(), e.getValue()));
            defFAC.applyParameters(params);

            log.debug(MolWitch.getModuleName() + " : " + smiles + " inchi = " + inchi);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
