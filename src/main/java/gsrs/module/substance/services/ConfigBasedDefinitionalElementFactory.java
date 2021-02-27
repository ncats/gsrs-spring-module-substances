package gsrs.module.substance.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import gsrs.module.substance.definitional.DefinitionalElement;
import gsrs.module.substance.definitional.DefinitionalElements;
import ix.ginas.models.v1.Substance;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

public class ConfigBasedDefinitionalElementFactory implements DefinitionalElementFactory{
    @Autowired
    private ConfigBasedDefinitionalElementConfiguration configuration;

    @Override
    public DefinitionalElements computeDefinitionalElementsFor(Substance s) {
        List<DefinitionalElement> list = new ArrayList<>();
        configuration.computeDefinitionalElements(s, list::add);
        return new DefinitionalElements(list);
    }

}
