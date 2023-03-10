package gsrs.module.substance.importers.model;

import gsrs.importer.PropertyBasedDataRecordContext;

public interface SDRecordContext extends PropertyBasedDataRecordContext {
    public String getStructure();

    public String getMolfileName();

}
