package gsrs.module.substance.processors;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.cv.api.*;
import gsrs.module.substance.services.CodeEntityService;
import ix.core.EntityProcessor;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * When a substance is saved and has an approvalID, check for a corresponding
 * Code. If necessary, create a new Code
 *
 * @author Mitch Miller
 */
@Slf4j
public class ApprovalIdProcessor implements EntityProcessor<Substance> {

    private String codeSystem;

    private CachedSupplier initializer = CachedSupplier.runOnceInitializer(this::addCodeSystemIfNeeded);

    public void setCodeSystem(String codeSystem) {
        this.codeSystem = codeSystem;
    }

    @Autowired
    private ControlledVocabularyApi api;

    @Autowired
    private CodeEntityService codeEntityService;


    private void addCodeSystemIfNeeded(){
        if(codeSystem ==null){
            return;
        }
        try {
            Optional<AbstractGsrsControlledVocabularyDTO> opt = api.findByDomain("CODE_SYSTEM");

        boolean addNew=true;
        if(opt.isPresent()){
            for(GsrsVocabularyTermDTO term : ((GsrsControlledVocabularyDTO)opt.get()).getTerms()){
                if (term.getValue().equals(codeSystem)) {
                    addNew = false;
                    break;
                }
            }
        }

        if(addNew) {
            List<CodeSystemTermDTO> list = new ArrayList<>();
            list.add(CodeSystemTermDTO.builder()
                    .display(codeSystem)
                    .value(codeSystem)
                    .hidden(true)
                    .build());

            api.create(GsrsCodeSystemControlledVocabularyDTO.builder()
                    .domain("CODE_SYSTEM")
                    .terms(list)
                    .build());

        }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
//    private void addCodeSystem()  {
//        Runnable r = new Runnable() {
//            @Override
//            public void run() {
//                log.trace("in addCodeSystem.run");
//                if (codeSystem != null) {
//                    Optional<GsrsCodeSystemControlledVocabularyDTO> cvvOpt;
//                    List<ControlledVocabulary> vocabList = repo.findByDomain("CODE_SYSTEM");
//                    log.trace("vocabList size: " + vocabList.size());
//                    ControlledVocabulary vocab = vocabList.get(0);
//                    boolean addNew = true;
//                    for (VocabularyTerm term : vocab.getTerms()) {
//                        if (term.getValue().equals(codeSystem)) {
//                            addNew = false;
//                            break;
//                        }
//                    }
//                    if (addNew) {
//                        Sneak.sneakyThrow(new Exception("Create code system '" + codeSystem+ "' within GSRS"));
//
////                        CodeSystemVocabularyTerm vt = new CodeSystemVocabularyTerm();
////                        vt.display = codeSystem;
////                        vt.value = codeSystem;
////                        vt.hidden = true;
////                        List<ControlledVocabulary> vocabs = repo.findByDomain(codeSystem);
////                        vocab.getTerms().add(vt);
////
////                        repo.saveAndFlush(vocab);
////                        log.trace("saved code system");
//                    }
//                }
//            }
//        };
//        r.run();
//    }

    @Override
    public void prePersist(Substance s) {
        copyCodeIfNecessary(s);
    }

    @Override
    public void preUpdate(Substance s) {
        log.trace("preUpdate");
        copyCodeIfNecessary(s);
    }

    public void copyCodeIfNecessary(Substance s) {
        if(codeSystem ==null){
            return;
        }
        initializer.getSync();
        log.trace("copyCodeIfNecessary. codeSystem: " + codeSystem);
        if (s.approvalID != null && s.approvalID.length() > 0) {
            log.trace("handling approval ID " + s.approvalID);
            boolean needCode = true;
            for (Code code : s.getCodes()) {
                if (codeSystem.equals(code.codeSystem)) {
                    if (code.code == null || code.code.length() == 0 || !code.code.equals(s.approvalID)) {
                        code.code = s.approvalID;
                        code.deprecate(true);
                        log.trace("deleted old code");
                    }
                    else if (code.code != null && code.code.equals(s.approvalID)) {
                        //don't put a break here because there may be other codes to be deprecated
                        needCode = false;
                    }
                }
            }
            if (needCode) {
                codeEntityService.createNewCode(s, codeSystem, s.approvalID);

                log.trace("Added new code for approvalId");
            }
        }
    }

    @Override
    public Class<Substance> getEntityClass() {
        return Substance.class;
    }

    public String getCodeSystem() {
        return codeSystem;
    }

}
