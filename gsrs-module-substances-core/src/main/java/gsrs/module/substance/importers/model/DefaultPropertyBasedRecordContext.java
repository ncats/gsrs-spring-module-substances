package gsrs.module.substance.importers.model;

import java.util.*;
import java.util.stream.Collectors;

public class DefaultPropertyBasedRecordContext implements PropertyBasedDataRecordContext {

    private Map<String, String> properties = new HashMap<>();

    public void setProperty(String propertyName, String value){
        this.properties.put(propertyName, value);
    }

    public void setAllProperties(Map<String, String> values) {
        this.properties.clear();
        this.properties.putAll(values);
    }

    @Override
    public Optional<String> getProperty(String name) {
        return Optional.ofNullable( properties.getOrDefault(name, null));
    }

    @Override
    public List<String> getProperties() {
        return properties.keySet().stream().collect(Collectors.toList());
    }

    @Override
    public Map<String, String> getSpecialPropertyMap() {
        return properties;
    }

    @Override
    public Optional<String> resolveSpecial(String name) {
        if (name.startsWith("UUID_")) {
            String ret = properties.computeIfAbsent(name, k -> {
                return UUID.randomUUID().toString();
            });
            return Optional.ofNullable(ret);
        }
        return Optional.empty();
    }
}
