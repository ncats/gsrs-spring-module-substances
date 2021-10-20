package gsrs.module.substance.autoconfigure;

import gov.nih.ncats.molwitch.Chemical;
import gov.nih.ncats.molwitch.ChemicalBuilder;
import gov.nih.ncats.molwitch.MolWitch;
import gov.nih.ncats.molwitch.inchi.Inchi;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class MolwitchLoader implements ApplicationListener<ContextRefreshedEvent> {

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        Chemical build = null;
        try {
            build = ChemicalBuilder.createFromSmiles("O=C=O").build();

            String smiles = build.toSmiles();

            String inchi = Inchi.asStdInchi(build).getKey();

            System.out.println(MolWitch.getModuleName() + " : " + smiles + " inchi = " + inchi);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
