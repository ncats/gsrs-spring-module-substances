package example.imports;

import gsrs.dataexchange.model.*;
import gsrs.importer.DefaultPropertyBasedRecordContext;
import gsrs.importer.PropertyBasedDataRecordContext;
import gsrs.module.substance.importers.importActionFactories.NoOpActionFactory;
import gsrs.module.substance.importers.importActionFactories.NotesExtractorActionFactory;
import ix.core.util.EntityUtils;
import ix.ginas.modelBuilders.AbstractSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ImporterAdapterTest {

    @Test
    public void testMappingParameterBuilder() {
        String itemName="Name1";
        Class itemType=String.class;
        String itemValue="Value1";
        MappingParameterBuilder builder = MappingParameterBuilder.instance();
        builder.setFieldName(itemName)
                .setValueType(itemType)
                .setDefaultValue(itemValue);
        MappingParameter parameter = builder.build();

        Assertions.assertEquals(itemName, parameter.getFieldName());
        Assertions.assertEquals(itemType, parameter.getValueType());
        Assertions.assertEquals(itemValue, parameter.getDefaultValue());
    }

    @Test
    public void testMappingActionFactoryMetadataBuilder() {
        String itemName="New Name 1";
        Class itemType= List.class;
        String itemValue="New Value 1";
        String operationName = "Create Something";
        MappingParameterBuilder parameterBuilder = MappingParameterBuilder.instance();
        parameterBuilder.setFieldName(itemName)
                .setValueType(itemType)
                .setDefaultValue(itemValue);
        MappingParameter parameter = parameterBuilder.build();

        MappingActionFactoryMetadataBuilder builder = MappingActionFactoryMetadataBuilder.instance();
        MappingActionFactoryMetadata metadata= builder.setLabel(operationName)
                .addParameterField(parameter)
                .build();
        Assertions.assertEquals(itemName, metadata.getParameterFields().get(0).getFieldName());
        Assertions.assertEquals(operationName, metadata.getLabel());
    }

    @Test
    public void testRequiredVsNot() {
        NotesExtractorActionFactory factory = new NotesExtractorActionFactory();
        List<MappingParameter> allParms= factory.getMetadata().getParameterFields();
        List<MappingParameter> requiredParms= factory.getMetadata().getParameterFields()
                .stream()
                .filter(i->i.isRequired()).collect(Collectors.toList());
        Assertions.assertEquals(requiredParms.size()+1, allParms.size());
    }

    @Test
    public void testNoOpAction() throws Exception {
        SubstanceBuilder builder = new SubstanceBuilder();
        Reference reference1 = new Reference();
        reference1.docType="WEBSITE";
        reference1.citation="https://www.google.com";

        Name basicName = new Name();
        basicName.name="Some Substance";
        basicName.type ="cn";
        basicName.addReference(reference1);
        builder.addName(basicName);
        builder.addReference(reference1);

        Code code1 = new Code();
        code1.code="50-00-0";
        code1.codeSystem="CAS";
        code1.type= "PRIMARY";
        builder.addCode(code1);

        NoOpActionFactory factory = new NoOpActionFactory();
        Map<String, Object> parameters = new HashMap<>();

        MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext> action= factory.create(parameters);
        DefaultPropertyBasedRecordContext ctx = new DefaultPropertyBasedRecordContext();
        String absentCode="2244";
        ctx.setProperty("CID", absentCode);
        String absentName= "Another name";
        ctx.setProperty("Primary Name", absentName);
        SubstanceBuilder outputBuilder= (SubstanceBuilder) action.act(builder, ctx);
        Substance finalSubstance = outputBuilder.build();
        Assertions.assertEquals(1, finalSubstance.names.size());
        Assertions.assertEquals(1, finalSubstance.codes.size());
        EntityUtils.EntityWrapper wrapper = EntityUtils.EntityWrapper.of(finalSubstance);
        String substanceJson =wrapper.toFullJson();
        Assertions.assertFalse(substanceJson.contains(absentName));
        Assertions.assertFalse(substanceJson.contains(absentCode));
    }
}