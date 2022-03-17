package example.imports;

import gsrs.module.substance.importers.SDFImportAdaptorFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

public class ImporterAdapterTest {

    @Test
    public void testMappingParameterBuilder() {
        String itemName="Name1";
        Class itemType=String.class;
        String itemValue="Value1";
        SDFImportAdaptorFactory.MappingParameterBuilder builder = SDFImportAdaptorFactory.MappingParameterBuilder.instance();
        builder.setFieldName(itemName)
                .setValueType(itemType)
                .setDefaultValue(itemValue);
        SDFImportAdaptorFactory.MappingParameter parameter = builder.build();

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
        SDFImportAdaptorFactory.MappingParameterBuilder parameterBuilder = SDFImportAdaptorFactory.MappingParameterBuilder.instance();
        parameterBuilder.setFieldName(itemName)
                .setValueType(itemType)
                .setDefaultValue(itemValue);
        SDFImportAdaptorFactory.MappingParameter parameter = parameterBuilder.build();

        SDFImportAdaptorFactory.MappingActionFactoryMetadataBuilder builder = SDFImportAdaptorFactory.MappingActionFactoryMetadataBuilder.instance();
        SDFImportAdaptorFactory.MappingActionFactoryMetadata metadata= builder.setLabel(operationName)
                .addParameterField(parameter)
                .build();
        Assertions.assertEquals(itemName, metadata.getParameterFields().get(0).getFieldName());
        Assertions.assertEquals(operationName, metadata.getLabel());
    }

    @Test
    public void testRequiredVsNot() {
        SDFImportAdaptorFactory.NotesExtractorActionFactory factory = new SDFImportAdaptorFactory.NotesExtractorActionFactory();
        List<SDFImportAdaptorFactory.MappingParameter> allParms= factory.getMetadata().getParameterFields();
        List<SDFImportAdaptorFactory.MappingParameter> requiredParms= factory.getMetadata().getParameterFields()
                .stream()
                .filter(i->i.isRequired()).collect(Collectors.toList());
        Assertions.assertEquals(requiredParms.size()+1, allParms.size());
    }
}
