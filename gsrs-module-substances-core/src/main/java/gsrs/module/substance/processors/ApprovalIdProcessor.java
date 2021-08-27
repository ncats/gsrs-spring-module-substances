package gsrs.module.substance.processors;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.cv.api.*;
import ix.core.EntityProcessor;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Substance;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * When a substance is saved and has an approvalID, check for a corresponding
 * Code. If necessary, create a new Code
 *
 * @author Mitch Miller
 */
@Slf4j
public class ApprovalIdProcessor implements EntityProcessor<Substance> {

    private String codeSystem;

    private final CachedSupplier initializer = CachedSupplier.runOnceInitializer(this::addCodeSystemIfNeeded);

    public void setCodeSystem(String codeSystem) {
        this.codeSystem = codeSystem;
    }

    private final String CV_DOMAIN = "CODE_SYSTEM";

    @Autowired
    private ControlledVocabularyApi api;

    private void addCodeSystemIfNeeded() {
        addCodeSystemIfNeeded(api, codeSystem, CV_DOMAIN);
    }

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
        if (codeSystem == null) {
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
                Code newCode = new Code(codeSystem, s.approvalID);
                s.addCode(newCode);
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

    private void addCodeSystemIfNeeded(ControlledVocabularyApi api, String codeSystem, String cvDomain) {
        log.trace("starting addCodeSystemIfNeeded");
        try {
            Optional<GsrsCodeSystemControlledVocabularyDTO> opt = api.findByDomain(cvDomain);

            boolean addNew = true;
            if (opt.isPresent()) {
                log.trace("CV_DOMAIN found");
                for (CodeSystemTermDTO term : ( opt.get()).getTerms()) {
                    if (term.getValue().equals(codeSystem)) {
                        addNew = false;
                        break;
                    }
                }
                log.trace("addNew: " + addNew);
                if (addNew) {
                    List<CodeSystemTermDTO> list = opt.get().getTerms().stream()
                            .map(t -> (CodeSystemTermDTO) t).collect(Collectors.toList());
                    list.add(CodeSystemTermDTO.builder()
                            .display(codeSystem)
                            .value(codeSystem)
                            .hidden(true)
                            .build());

                    opt.get().setTerms(list);
                    //the following line prevents an exception while saving the CV
                    //todo: figure out why this is necessary
                    opt.get().setVocabularyTermType("ix.ginas.models.v1.CodeSystemControlledVocabulary");
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
