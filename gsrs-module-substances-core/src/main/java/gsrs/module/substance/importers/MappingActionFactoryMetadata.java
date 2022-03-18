package gsrs.module.substance.importers;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
@Data
public class MappingActionFactoryMetadata {
    private String label;
    private List<MappingParameter> parameterFields;

    public MappingActionFactoryMetadataBuilder builder() {
        return MappingActionFactoryMetadataBuilder.instance();
    }

    public MappingActionFactoryMetadata(MappingActionFactoryMetadataBuilder builder) {
        this.label = builder.label;
        this.parameterFields = builder.parameterFields;
    }

    public static class MappingActionFactoryMetadataBuilder {
        private String label;
        private List<MappingParameter> parameterFields;

        public MappingActionFactoryMetadataBuilder setLabel(String label) {
            this.label = label;
            return this;
        }

        public MappingActionFactoryMetadataBuilder setParameterFields(List<MappingParameter> parameters) {
            this.parameterFields = parameters;
            return this;
        }

        public MappingActionFactoryMetadataBuilder addParameterField(MappingParameter parameter) {
            this.parameterFields.add(parameter);
            return this;
        }

        public MappingActionFactoryMetadataBuilder() {
            this.parameterFields = new ArrayList<>();
        }

        public static MappingActionFactoryMetadataBuilder instance() {
            return new MappingActionFactoryMetadataBuilder();
        }

        public MappingActionFactoryMetadata build() {
            return new MappingActionFactoryMetadata(this);
        }
    }
}