package gsrs.module.substance.importers.model;

import gov.nih.ncats.molwitch.Chemical;

import java.util.*;
import java.util.stream.Collectors;

public class ChemicalBackedSDRecordContext implements SDRecordContext {
    private Chemical c;

    /*
    specialProperties is a Map under the direct control of this class, in addition to the properties Map within the
    Chemical object, which is immutable and shared with other consumers of Molwitch.
     */
    private Map<String, String> specialProperties = new HashMap<>();

    public ChemicalBackedSDRecordContext(Chemical c) {
        this.c = c;
    }

    @Override
    public String getStructure() {
        try {
            return c.toMol();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Optional<String> getProperty(String name) {
        return Optional.ofNullable(c.getProperty(name));
    }

    @Override
    public List<String> getProperties() {
        return c.getProperties().keySet().stream().collect(Collectors.toList());
    }

    @Override
    public Map<String, String> getSpecialPropertyMap() {
        return this.specialProperties;
    }

    @Override
    public String getMolfileName() {
        return c.getName();
    }
}
