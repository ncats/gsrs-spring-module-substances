package gsrs.module.substance.processors;

import gsrs.module.substance.repository.StructureRepository;
import gsrs.module.substance.repository.ValueRepository;
import gsrs.module.substance.services.RecalcStructurePropertiesService;
import ix.core.EntityProcessor;
import ix.core.chem.StructureProcessor;
import ix.core.models.Structure;
import ix.core.models.Value;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ChemicalStructurePropertiesProcessor implements EntityProcessor<Substance> {
    @Autowired
    private RecalcStructurePropertiesService recalcStructurePropertiesService;

    @Override
    public void prePersist(Substance obj) throws FailProcessingException {
        if(obj instanceof ChemicalSubstance){
            generateStructureProperties((ChemicalSubstance)obj);
        }
    }

    @Override
    public void preUpdate(Substance obj) throws FailProcessingException {
        if(obj instanceof ChemicalSubstance){
            generateStructureProperties((ChemicalSubstance)obj);
        }
    }

    private void generateStructureProperties(ChemicalSubstance substance){
        recalcStructurePropertiesService.recalcStructureProperties(substance.structure);
    }


    @Override
    public Class<Substance> getEntityClass() {
        return Substance.class;
    }
}
