package gsrs.module.substance.processors;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.cv.api.*;
import gsrs.springUtils.AutowireHelper;
import ix.core.EntityProcessor;
import ix.ginas.models.v1.*;
import ix.ginas.utils.CodeSequentialGenerator;
import ix.ginas.utils.validation.ValidationUtils;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class UniqueCodeGenerator implements EntityProcessor<Substance> {

    private CodeSequentialGenerator seqGen = null;
    private String codeSystem;
    private final String CODE_SYSTEM_VOCABULARY = "CODE_SYSTEM";

    private final CachedSupplier initializer = CachedSupplier.runOnceInitializer(this::addCodeSystem);
    
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
        //addCodeSystem();
    }

    private void addCodeSystem() {
        ValidationUtils.addCodeSystemIfNeeded(cvApi, codeSystem, CODE_SYSTEM_VOCABULARY);
    }

    public void generateCodeIfNecessary(Substance s) {
        log.trace("starting in generateCodeIfNecessary");
        initializer.getSync();

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
                    log.trace("Generating new code for substance" );
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
