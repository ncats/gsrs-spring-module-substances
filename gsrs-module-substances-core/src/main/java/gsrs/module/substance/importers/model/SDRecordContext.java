package gsrs.module.substance.importers.model;

import java.util.List;
import java.util.Optional;

public interface SDRecordContext extends PropertyBasedDataRecordContext{
    public String getStructure();

    public String getMolfileName();

}
