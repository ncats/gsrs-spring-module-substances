package gsrs.module.substance.processors;

import gsrs.cv.api.CodeSystemTermDTO;
import gsrs.cv.api.ControlledVocabularyApi;
import gsrs.cv.api.GsrsCodeSystemControlledVocabularyDTO;
import ix.core.EntityProcessor;
import ix.ginas.models.v1.*;
import ix.ginas.utils.CodeSequentialGenerator;
import java.io.IOException;

import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class UniqueCodeGenerator implements EntityProcessor<Substance> {

    private CodeSequentialGenerator seqGen = null;
    private String codeSystem;
    private final String CODE_SYSTEM_VOCABULARY = "CODE_SYSTEM";

    public UniqueCodeGenerator() {
    }

    @Autowired
    private ControlledVocabularyApi cvApi;

    public UniqueCodeGenerator(Map with) {
        log.trace("UniqueCodeGenerator constructor with Map");
        String name = (String) with.get("name");
        codeSystem = (String) with.get("codesystem");
        String codeSystemSuffix = (String) with.get("suffix");
        Long last = (Long) with.computeIfPresent("last", (key, val) -> ((Number) val).longValue());
        int length = (Integer) with.get("length");
        boolean padding = (Boolean) with.get("padding");
        if (codeSystem != null) {
            seqGen = new CodeSequentialGenerator(name, length, codeSystemSuffix, padding, codeSystem);//removed 'last'
        }
        addCodeSystem();
    }

    private void addCodeSystem() {
        if (codeSystem != null) {
            Optional<GsrsCodeSystemControlledVocabularyDTO> opt = null;
            try {
                opt = cvApi.findByDomain(CODE_SYSTEM_VOCABULARY);
                if (opt.isPresent()) {
                    GsrsCodeSystemControlledVocabularyDTO vocab = opt.get();
                    boolean addNew = true;
                    for (CodeSystemTermDTO vt1 : vocab.getTerms()) {
                        if (vt1.getValue().equals(codeSystem)) {
                            addNew = false;
                            break;
                        }
                    }
                    if (addNew) {
                        CodeSystemTermDTO vt
                                = CodeSystemTermDTO.builder()
                                        .display(codeSystem)
                                        .value(codeSystem)
                                        .hidden(true)
                                        .build();

                        //*************************************
                        // This causes problems if done first
                        // may have ramifications elsewhere
                        //*************************************
                        //vt.save();
                        vocab.getTerms().add(vt);
                        cvApi.update(vocab);
                        log.debug("done adding code system CV");
                    }

                }
            } catch (IOException ex) {
                log.error("Error creating code CV: ", ex);
                ex.printStackTrace();
            }

            //System.out.println("Done adding code system");
        }

        //GinasGlobal.runAfterStart(r);
    }

    public void generateCodeIfNecessary(Substance s) {
        log.trace("starting in generateCodeIfNecessary");
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
                    //System.out.println("Generating new code:" + c.code);
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
    public void postPersist(Substance obj) {
        // TODO Auto-generated method stub

    }

    @Override
    public void preRemove(Substance obj) {
        // TODO Auto-generated method stub

    }

    @Override
    public void postRemove(Substance obj) {
        // TODO Auto-generated method stub

    }

    @Override
    public void preUpdate(Substance obj) {
        prePersist(obj);
    }

    @Override
    public void postUpdate(Substance obj) {
        // TODO Auto-generated method stub

    }

    @Override
    public void postLoad(Substance obj) {
        // TODO Auto-generated method stub

    }

    @Override
    public Class<Substance> getEntityClass() {
        return Substance.class;
    }
}
