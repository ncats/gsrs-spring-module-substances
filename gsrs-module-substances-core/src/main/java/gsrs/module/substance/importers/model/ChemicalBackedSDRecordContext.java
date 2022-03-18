package gsrs.module.substance.importers.model;

import gov.nih.ncats.molwitch.Chemical;

import java.util.*;
import java.util.stream.Collectors;

public class ChemicalBackedSDRecordContext implements SDRecordContext {
    private Chemical c;
    private Map<String, String> specialProps = new HashMap<>();

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
    public Optional<String> resolveSpecial(String name) {

        if (name.startsWith("UUID_")) {
            String ret = specialProps.computeIfAbsent(name, k -> {
                return UUID.randomUUID().toString();
            });
            return Optional.ofNullable(ret);
        }
        return Optional.empty();
    }

    @Override
    public String getMolfileName() {
        return c.getName();
    }
}
