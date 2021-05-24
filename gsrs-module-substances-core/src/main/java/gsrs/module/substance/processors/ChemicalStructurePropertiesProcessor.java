package gsrs.module.substance.processors;

import gsrs.module.substance.repository.StructureRepository;
import gsrs.module.substance.repository.ValueRepository;
import ix.core.EntityProcessor;
import ix.core.chem.StructureProcessor;
import ix.core.models.Structure;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;

public class ChemicalStructurePropertiesProcessor implements EntityProcessor<Substance> {
    @Autowired
    private StructureProcessor structureProcessor;

    @Autowired
    private StructureRepository structureRepository;

    @Autowired
    private ValueRepository valueRepository;

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
        Structure s = substance.structure;
        Structure newStructure = structureProcessor.instrument(s.molfile);
        s.updateStructureFields(newStructure);
        s.properties.forEach(valueRepository::save);
        structureRepository.save(s);
    }

    @Override
    public Class<Substance> getEntityClass() {
        return Substance.class;
    }
}
