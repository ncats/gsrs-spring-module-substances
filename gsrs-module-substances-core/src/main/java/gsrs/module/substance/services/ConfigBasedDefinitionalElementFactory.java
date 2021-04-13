package gsrs.module.substance.services;

import gsrs.module.substance.definitional.DefinitionalElement;
import gsrs.module.substance.definitional.DefinitionalElements;
import ix.ginas.models.v1.Substance;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ConfigBasedDefinitionalElementFactory implements DefinitionalElementFactory{
    @Autowired
    private ConfigBasedDefinitionalElementConfiguration configuration;

    @Override
    public DefinitionalElements computeDefinitionalElementsFor(Substance s) {
        List<DefinitionalElement> list = new ArrayList<>();
        addDefinitionalElementsFor(s, list::add);
        return new DefinitionalElements(list);
    }

    @Override
    public void addDefinitionalElementsFor(Object o, Consumer<DefinitionalElement> consumer) {
        configuration.computeDefinitionalElements(o, consumer);
    }

}
