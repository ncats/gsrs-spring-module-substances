package example.imports;

import gsrs.dataexchange.model.MappingAction;
import gsrs.importer.DefaultPropertyBasedRecordContext;
import gsrs.importer.PropertyBasedDataRecordContext;
import gsrs.module.substance.importers.importActionFactories.CodeExtractorActionFactory;
import ix.ginas.modelBuilders.AbstractSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class CodeExtractorActionFactoryTest {

    @Test
    public void testParseCode1() throws Exception {
        CodeExtractorActionFactory codeExtractorActionFactory = new CodeExtractorActionFactory();

        SubstanceBuilder substanceBuilder = new SubstanceBuilder();

        DefaultPropertyBasedRecordContext ctx = new DefaultPropertyBasedRecordContext();

        Map<String, Object> inputParams = new HashMap<>();

        inputParams.put("codeSystem", "PUBCHEM_COMPOUND_CID");
        inputParams.put("code", "2244");
        inputParams.put("codeType", "PRIMARY");

        MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext> action= codeExtractorActionFactory.create(inputParams);
        action.act(substanceBuilder, ctx);
        Assertions.assertEquals(1, substanceBuilder.build().codes.size());
        Assertions.assertEquals("2244", substanceBuilder.build().codes.get(0).code);
        Assertions.assertEquals(0, substanceBuilder.build().codes.get(0).getReferencesAsUUIDs().size());
    }

    @Test
    public void testParseCode2() throws Exception {
        CodeExtractorActionFactory codeExtractorActionFactory = new CodeExtractorActionFactory();

        SubstanceBuilder substanceBuilder = new SubstanceBuilder();

        DefaultPropertyBasedRecordContext ctx = new DefaultPropertyBasedRecordContext();
        ctx.setProperty("CID_VALUE", "2244");

        Map<String, Object> inputParams = new HashMap<>();

        inputParams.put("codeSystem", "PUBCHEM_COMPOUND_CID");
        inputParams.put("code", "{{CID_VALUE}}");
        inputParams.put("codeType", "PRIMARY");

        MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext> action= codeExtractorActionFactory.create(inputParams);
        action.act(substanceBuilder, ctx);
        Assertions.assertEquals(1, substanceBuilder.build().codes.size());
        Assertions.assertEquals("2244", substanceBuilder.build().codes.get(0).code);
        //Assertions.assertEquals(0, substanceBuilder.build().codes.get(0).getReferencesAsUUIDs().size());
    }

}
