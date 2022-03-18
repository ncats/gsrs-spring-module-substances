package gsrs.module.substance.importers.actions;

import gsrs.module.substance.importers.MappingActionFactoryMetadata;
import gsrs.module.substance.importers.SDFImportAdaptorFactory;

import java.util.Map;

public interface ImportMappingActionFactory<T, U> {
    public ImportMappingAction<T, U> create(Map<String, Object> params);

    public MappingActionFactoryMetadata getMetadata();

}