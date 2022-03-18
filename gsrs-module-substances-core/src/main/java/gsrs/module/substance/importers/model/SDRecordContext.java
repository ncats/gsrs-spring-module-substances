package gsrs.module.substance.importers.model;

import java.util.List;
import java.util.Optional;

public interface SDRecordContext {
    public String getStructure();

    public String getMolfileName();

    public Optional<String> getProperty(String name);

    public List<String> getProperties();

    public Optional<String> resolveSpecial(String name);
}
