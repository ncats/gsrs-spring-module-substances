/*
	* When a substance is saved and has an approvalID, check for a corresponding Code.
	* If necessary, create a new Code
 */
package gsrs.module.substance.processors;

import ix.core.EntityProcessor;

import gsrs.cv.api.CodeSystemTermDTO;
import gsrs.cv.api.ControlledVocabularyApi;
import gsrs.cv.api.GsrsCodeSystemControlledVocabularyDTO;
import gsrs.repository.ControlledVocabularyRepository;
import ix.ginas.models.v1.*;
import ix.ginas.models.v1.CodeSystemVocabularyTerm;
import java.io.IOException;
import java.util.List;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author Mitch Miller
 */
public class ApprovalIdProcessor implements EntityProcessor<Substance> {

    @Value("${ix.gsrs.vocabulary.ApprovalIDCodeSystem:FDA UNII}")
    private String codeSystem = "";

    @Autowired
    private ControlledVocabularyApi cvApi;

    @Autowired
    ControlledVocabularyRepository repo;

    public ApprovalIdProcessor() {
        if (null == codeSystem || codeSystem.length() == 0) {
            System.out.println("codeSystem was null/empty!");
            codeSystem = "FDA UNII";
        }
        addCodeSystem();
    }

    private void addCodeSystem() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (codeSystem != null) {
                    Optional<GsrsCodeSystemControlledVocabularyDTO> cvvOpt;
                    try {
                        cvvOpt = cvApi.findByDomain("CODE_SYSTEM");
                        if (!cvvOpt.isPresent()) {
                            return;
                        }
                        GsrsCodeSystemControlledVocabularyDTO cvv = cvvOpt.get();
                        boolean addNew = true;
                        for (CodeSystemTermDTO vt1 : cvv.getTerms()) {
                            if (vt1.getValue().equals(codeSystem)) {
                                addNew = false;
                                break;
                            }
                        }
                        if (addNew) {

                            CodeSystemVocabularyTerm vt = new CodeSystemVocabularyTerm();
                            vt.display = codeSystem;
                            vt.value = codeSystem;
                            vt.hidden = true;
                            /*CodeSystemTermDTO term = new CodeSystemTermDTO();
                        term.setDisplay(codeSystem);
                        term.setValue(codeSystem);
                        term.setHidden(true);*/

                            //*************************************
                            // This causes problems if done first
                            // may have ramifications elsewhere
                            //*************************************
                            //vt.save();
                            //cvv.getTerms().add(term);
                            //ControlledVocabulary cv = new CodeSystemControlledVocabulary();
                            List<ControlledVocabulary> vocabs = repo.findByDomain(codeSystem);
                            //assume there's one 
                            ControlledVocabulary vocab = vocabs.get(0);
                            vocab.getTerms().add(vt);

                            repo.saveAndFlush(vocab);

                            //Needed because update doesn't necessarily
                            //trigger the update hooks
                            //EntityPersistAdapter.getInstance().reindex(cvv);
                        }

                    } catch (IOException ex) {
                        Logger.getLogger(ApprovalIdProcessor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        };
        //GinasGlobal.runAfterStart(r);

    }

    @Override
    public void prePersist(Substance s) {
        copyCodeIfNecessary(s);
    }

    @Override
    public void preUpdate(Substance obj) {
		System.out.println("preUpdate");
        prePersist(obj);
    }

    public void copyCodeIfNecessary(Substance s) {
        if (s.approvalID != null && s.approvalID.length() > 0) {
            Logger.getLogger(this.getClass().getName()).log(Level.FINE, "handling approval ID " + s.approvalID);
            System.out.println("handling approval ID " + s.approvalID);
            boolean needCode = true;
            for (Code code : s.getCodes()) {
                if (code.codeSystem.equals(codeSystem)) {
                    if (code.code == null || code.code.length() == 0 || !code.code.equals(s.approvalID)) {
                        code.code = s.approvalID;
                        code.setDeprecated(true);
                        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "deleted old code");
                    }
                    else if (code.code != null && code.code.equals(s.approvalID)) {
                        needCode = false;
                    }
                }
            }
            if (needCode) {
                Code newCode = new Code(codeSystem, s.approvalID);
                s.codes.add(newCode);
                Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Added new code for approvalId");
                System.out.println("Added new code for approvalId");
            }
        }
    }

    @Override
    public Class<Substance> getEntityClass() {
        return Substance.class;
    }
}
