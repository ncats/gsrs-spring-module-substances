package gsrs.module.substance.processors;

import gov.nih.ncats.common.sneak.Sneak;
import ix.core.EntityProcessor;
import gsrs.cv.api.GsrsCodeSystemControlledVocabularyDTO;
import gsrs.repository.ControlledVocabularyRepository;
import ix.ginas.models.v1.*;
import ix.ginas.models.v1.CodeSystemVocabularyTerm;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * When a substance is saved and has an approvalID, check for a corresponding
 * Code. If necessary, create a new Code
 *
 * @author Mitch Miller
 */
@Slf4j
public class ApprovalIdProcessor implements EntityProcessor<Substance> {

    private String codeSystem;

// autoriring ControlledVocabularyRepository  causes exception as of 12 Aug 2021
//    @Autowired
//    ControlledVocabularyRepository repo;

    public ApprovalIdProcessor(Map with) {
        if (with!=null && with.get("codeSystem")!= null ) {
            codeSystem = (String) with.get("codeSystem");
//            log.trace("codeSystem was null/empty!");
//            codeSystem = "FDA UNII";
        }
        //todo: find a way to access CVs programmatically 12 Aug 2021
        // rely on the admin/user to make sure the code system exists
        //addCodeSystem();
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
    public void preUpdate(Substance obj) {
        log.trace("preUpdate");
        prePersist(obj);
    }

    public void copyCodeIfNecessary(Substance s) {
        log.trace("copyCodeIfNecessary. codeSystem: " + codeSystem);
        if (s.approvalID != null && s.approvalID.length() > 0) {
            log.trace("handling approval ID " + s.approvalID);
            boolean needCode = true;
            for (Code code : s.getCodes()) {
                if (code.codeSystem.equals(codeSystem)) {
                    if (code.code == null || code.code.length() == 0 || !code.code.equals(s.approvalID)) {
                        code.code = s.approvalID;
                        code.setDeprecated(true);
                        log.trace("deleted old code");
                    }
                    else if (code.code != null && code.code.equals(s.approvalID)) {
                        needCode = false;
                    }
                }
            }
            if (needCode) {
                Code newCode = new Code(codeSystem, s.approvalID);
                s.codes.add(newCode);
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
