package example.imports;

import gsrs.module.substance.importers.SDFImportAdaptorFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ImporterAdapterTest {

    @Test
    public void testMetadatBuilder() {
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

}
