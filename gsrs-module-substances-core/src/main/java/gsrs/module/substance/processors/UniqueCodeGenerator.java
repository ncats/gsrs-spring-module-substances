package gsrs.module.substance.processors;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.cv.api.*;
import gsrs.springUtils.AutowireHelper;
import ix.core.EntityProcessor;
import ix.ginas.models.v1.*;
import ix.ginas.utils.CodeSequentialGenerator;
import java.io.IOException;
import java.util.List;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class UniqueCodeGenerator implements EntityProcessor<Substance> {

    private CodeSequentialGenerator seqGen = null;
    private final String codeSystem;
    private final String CODE_SYSTEM_VOCABULARY = "ACCESS_GROUP";

    private final CachedSupplier initializer = CachedSupplier.runOnceInitializer(this::addCodeSystem);

    @Autowired
    private ControlledVocabularyApi cvApi;

    public UniqueCodeGenerator(Map with) {
        log.trace("UniqueCodeGenerator constructor with Map");
        String name = (String) with.get("name");
        codeSystem = (String) with.get("codesystem");
        String codeSystemSuffix = (String) with.get("suffix");
        //Long last = (Long) with.computeIfPresent("last", (key, val) -> ((Number) val).longValue());
        int length = (Integer) with.get("length");
        boolean padding = (Boolean) with.get("padding");
        String msg = String.format("codeSystem: %s; codeSystemSuffix: %s; length: %d;  padding: %b", 
                codeSystem, codeSystemSuffix, length, padding);
        log.trace(msg);
        if (codeSystem != null) {
            seqGen = new CodeSequentialGenerator(name, length, codeSystemSuffix, padding, codeSystem);//removed 'last'
        }
        try {
            initializer.getSync();
        }catch(Exception e) {
            log.warn("Error initializing codesystem generator, will be attempted again as needed");
        }
        //addCodeSystem();
    }

    private void addCodeSystem() {
        log.trace("addCodeSystem");
        addCodeSystemIfNeeded(cvApi, codeSystem, CODE_SYSTEM_VOCABULARY);
    }

    public void generateCodeIfNecessary(Substance s) {
        log.trace("starting in generateCodeIfNecessary");
        initializer.getSync();
        if(seqGen!=null){
            AutowireHelper.getInstance().autowire(seqGen);
            log.trace("autowired seqGen");
        }

        if (seqGen != null && s.isPrimaryDefinition()) {
            boolean hasCode = false;
            for (Code c : s.codes) {
                if (c.codeSystem.equals(seqGen.getCodeSystem())) {
                    hasCode = true;
                    break;
                }
            }
            if (!hasCode) {
                try {
                    seqGen.addCode(s);
                    log.trace("Generating new code for substance");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void prePersist(Substance s) {
        generateCodeIfNecessary(s);
    }

    @Override
    public void preUpdate(Substance s) {
        generateCodeIfNecessary(s);
    }

    @Override
    public Class<Substance> getEntityClass() {
        return Substance.class;
    }

    private static void addCodeSystemIfNeeded(ControlledVocabularyApi api, String codeSystem, String cvDomain) {
        log.trace("starting addCodeSystemIfNeeded. cvDomain: " + cvDomain + "; codeSystem: " + codeSystem);
        try {
            Optional<GsrsControlledVocabularyDTO> opt = api.findByDomain(cvDomain);

            boolean addNew = true;
            if (opt.isPresent()) {
                log.trace("CV_DOMAIN found");
                for (GsrsVocabularyTermDTO term : (opt.get()).getTerms()) {
                    if (term.getValue().equals(codeSystem)) {
                        addNew = false;
                        break;
                    }
                }
                log.trace("addNew: " + addNew);
                if (addNew) {
                    List<GsrsVocabularyTermDTO> list = opt.get().getTerms().stream()
                            .map(t -> (GsrsVocabularyTermDTO) t)
                            .collect(Collectors.toList());
                    list.add(GsrsVocabularyTermDTO.builder()
                            .display(codeSystem)
                            .value(codeSystem)
                            .hidden(true)
                            .build());

                    opt.get().setTerms(list);
                    //the following line prevents an exception while saving the CV
                    //todo: figure out why this is necessary
                    opt.get().setVocabularyTermType("ix.ginas.models.v1.ControlledVocabulary");
                    api.update(opt.get());
                    log.trace("saved updated CV");
                }
            }
            else {
                log.error("no code system CV found!");
                //todo: throw an exception
            }

        } catch (IOException e) {
            log.error("Error updating GSRS vocabulary: " + e.getMessage());
            e.printStackTrace();
        }

    }

}
