package gsrs.module.substance.importers.importActionFactories;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncats.molwitch.Chemical;
import gsrs.dataExchange.model.MappingAction;
import gsrs.dataExchange.model.MappingActionFactoryMetadata;
import gsrs.module.substance.importers.model.ChemicalBackedSDRecordContext;
import gsrs.module.substance.importers.model.SDRecordContext;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NSRSCASExtractorActionFactoryTest {

    @Test
    public void createTest() throws Exception {
        Map<String, Object> abstractParams = new HashMap<>();
        abstractParams.put("CASNumber", "50-00-0");
        abstractParams.put("codeType","PRIMARY");
        NSRSCASExtractorActionFactory nsrscasExtractorActionFactory = new NSRSCASExtractorActionFactory();
        MappingAction<Substance, SDRecordContext> action = nsrscasExtractorActionFactory.create(abstractParams);
        Chemical chem = Chemical.createFromSmilesAndComputeCoordinates("C=O");
        SDRecordContext record = new ChemicalBackedSDRecordContext(chem);

        Substance test = new Substance();
        Substance newChem =action.act(test, record);
        Assertions.assertTrue(newChem.getCodes().stream().anyMatch(c->c.codeSystem.equals("CAS") && c.code.equals("50-00-0")));
    }

    @Test
    public void getMetadataTest() {
        NSRSCASExtractorActionFactory nsrscasExtractorActionFactory = new NSRSCASExtractorActionFactory();
        MappingActionFactoryMetadata metadata = nsrscasExtractorActionFactory.getMetadata();
        ObjectMapper om = new ObjectMapper();
        try {
            System.out.println("metadata: " + om.writeValueAsString(metadata));
        } catch (JsonProcessingException e) {
            System.err.println("no metadata available.");;
        }
        Assertions.assertEquals(3, metadata.getParameterFields().size());
        Assertions.assertTrue(metadata.getParameterFields().stream().anyMatch(f->f.getLabel().equals("CAS Number")));
    }

}