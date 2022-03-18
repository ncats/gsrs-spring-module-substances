package gsrs.module.substance.importers;

import lombok.Data;

@Data
public class MappingParameter<T> {
    private String fieldName;
    private String label;
    private boolean required = false;
    private T defaultValue;
    private Class<T> valueType;

    public MappingParameter(MappingParameterBuilder builder) {
        this.fieldName = builder.fieldName;
        this.label = builder.label;
        this.required = builder.required;
        this.defaultValue = (T) builder.defaultValue;
        this.valueType = builder.valueType;
    }

    public static MappingParameterBuilder builder() {
        return new MappingParameterBuilder();
    }

    public static class MappingParameterBuilder<T> {
        private String fieldName;
        private String label;
        private boolean required = false;
        private T defaultValue;
        private Class<T> valueType;

        public MappingParameterBuilder setFieldName(String fieldName) {
            this.fieldName = fieldName;
            return this;
        }

        public MappingParameterBuilder setLabel(String label) {
            this.label = label;
            return this;
        }

        public MappingParameterBuilder setRequired(boolean r) {
            this.required = r;
            return this;
        }

        public MappingParameterBuilder setDefaultValue(T defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public MappingParameterBuilder setValueType(Class<T> t) {
            this.valueType = t;
            return this;
        }

        public MappingParameter build() {
            return new MappingParameter(this);
        }

        public static MappingParameterBuilder instance() {
            return new MappingParameterBuilder();
        }

        public MappingParameterBuilder setFieldNameAndLabel(String fieldName, String fieldLabel) {
            this.fieldName = fieldName;
            this.label = fieldLabel;
            return this;
        }

        private MappingParameterBuilder() {
        }
    }
}