package gsrs.module.substance.services;

import gsrs.module.substance.repository.StructureRepository;
import gsrs.module.substance.repository.ValueRepository;
import ix.core.chem.StructureProcessor;
import ix.core.models.Structure;
import ix.core.models.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
@Service
public class RecalcStructurePropertiesService {
    @Autowired
    private StructureProcessor structureProcessor;

    @Autowired
    private StructureRepository structureRepository;

    @Autowired
    private ValueRepository valueRepository;

    /**
     * Recalculate the Structure field and properties using the
     * StructureProcessor and delete the old ones and save the new
     * ones into the Repositories.
     *
     * @param s the Structure to recalculate, should exist in the database.
     */
//    @Transactional
    public void recalcStructureProperties(Structure s) {
        List<Value> toDelete = s.properties.stream().collect(Collectors.toList());
        Structure newStructure = structureProcessor.instrument(s.molfile);
        s.updateStructureFields(newStructure);
        s.properties.forEach(valueRepository::save);
        structureRepository.save(s);
        valueRepository.deleteAll(toDelete);
    }
}
