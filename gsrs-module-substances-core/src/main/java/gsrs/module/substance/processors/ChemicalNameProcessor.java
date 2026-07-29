package gsrs.module.substance.processors;

import gsrs.module.substance.services.IupacNameService;
import ix.core.EntityProcessor;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ChemicalNameProcessor implements EntityProcessor<Substance> {

    @Autowired
    private IupacNameService service;

    @Setter
    private String nameType = "sys";

    @Setter
    private String nameLang = "en";

    @Setter
    private List<String> forbiddenGroups = new ArrayList<>();

    public ChemicalNameProcessor() {
        this(new HashMap<>());
    }

    public ChemicalNameProcessor(Map<String, Object> parameters) {
        if( parameters == null) return;

        if( parameters.containsKey("nameType")) {
            setNameType((String)parameters.get("nameType"));
        }

        if( parameters.containsKey("nameLang")) {
            setNameLang((String)parameters.get("nameLang"));
        }

        if( parameters.containsKey("forbiddenGroups")) {
            LinkedHashMap<String, Object> groups = (LinkedHashMap<String, Object>) parameters.get("forbiddenGroups");
            List<String> groupNames = groups.values().stream()
                    .filter(Objects::nonNull)
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(String::toUpperCase)
                    .collect(Collectors.toList());
            setForbiddenGroups(groupNames);
            log.trace("set forbidden groups to {}", groupNames);
        }
    }

    @Override
    public void prePersist(Substance obj) throws FailProcessingException {
        log.trace("in ChemicalNameProcessor prePersist");
        if(obj instanceof ChemicalSubstance){
            ChemicalSubstance chem = (ChemicalSubstance) obj;
            if(chem.getStructure().getAccess() != null &&
                    chem.getStructure().getAccess().stream().map(g->g.name.toUpperCase()).anyMatch(g2->this.forbiddenGroups.contains(g2))){
                log.info("structure access contains a forbidden group");
                return;
            }
            try {
                service.ensureIupacName((ChemicalSubstance)obj, nameType, nameLang);
            }
            catch (IOException | InterruptedException  ex){
                log.error("Error looking up PubChem name for {}", obj.getOrGenerateUUID(), ex);
                Thread.currentThread().interrupt(); // if InterruptedException is possible
            }
        }
    }

    @Override
    public void preUpdate(Substance obj) throws FailProcessingException {
        log.trace("in ChemicalNameProcessor preUpdate");
        if(obj instanceof ChemicalSubstance){
            ChemicalSubstance chem = (ChemicalSubstance)obj;
            if(chem.getStructure().getAccess().stream().map(g->g.name.toUpperCase()).anyMatch(g2->this.forbiddenGroups.contains(g2))){
                log.info("structure access contains a forbidden group");
                return;
            }
            try {
                service.ensureIupacName((ChemicalSubstance)obj, nameType, nameLang);
            }
            catch (IOException | InterruptedException  ex){
                log.error("Error looking up PubChem name for {}", obj.getOrGenerateUUID(), ex);
                Thread.currentThread().interrupt(); // if InterruptedException is possible
            }
        }
    }

    @Override
    public Class<Substance> getEntityClass() {
        return Substance.class;
    }
}
