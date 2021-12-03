package ix.ginas.utils.validation.validators;

import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.module.substance.repository.SubstanceRepository.SubstanceSummary;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import ix.ginas.utils.validation.ValidationUtils;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author mitch
 */
@Slf4j
public class CodeUniquenessValidator extends AbstractValidatorPlugin<Substance> {

    private Set< String> codeSystemsForWarning;
    private Set<String> codeSystemsforError;

    @Autowired
    private SubstanceRepository substanceRepository;

    @Override
    public void validate(Substance s, Substance objold, ValidatorCallback callback) {
        log.trace("starting in validate. singletonCodeSystems: " + codeSystemsForWarning + "; strictlyUniqueCodeSystems: " 
            + codeSystemsforError);
        Iterator<Code> codesIter = s.codes.iterator();

        while (codesIter.hasNext()) {
            Code cd = codesIter.next();

            if (!cd.type.equalsIgnoreCase("PRIMARY")) {
                log.trace(String.format("skipping code of system %s and type: %s because it's not PRIMARY", 
                        cd.codeSystem, cd.type));
                continue;
            }
            if (( (codeSystemsForWarning != null && !codeSystemsForWarning.contains(cd.codeSystem)))
                    && (codeSystemsforError == null || !codeSystemsforError.contains(cd.codeSystem))) {
                log.trace(String.format("skipping code of system %s and type: %s", cd.codeSystem, cd.type));
                continue;
            }
            List<SubstanceRepository.SubstanceSummary> sr = substanceRepository.findByCodes_CodeAndCodes_CodeSystem(cd.code, cd.codeSystem);

            if (sr != null && !sr.isEmpty()) {
                log.trace("found some possible duplicates..");
                //TODO we only check the first hit?
                //would be nice to say instead of possible duplicate hit say we got X hits
                Iterator<SubstanceSummary> substanceIterator=sr.iterator();
                
                while( substanceIterator.hasNext())
                {
                    SubstanceRepository.SubstanceSummary s2= substanceIterator.next();
                    log.trace("s2.getUuid(): " + s2.getUuid());
                    if (s2.getUuid() != null && !s2.getUuid().equals(s.getUuid())) {
                        GinasProcessingMessage mes;
                        if(codeSystemsforError != null && codeSystemsforError.contains(cd.codeSystem)) {
                            mes = GinasProcessingMessage
                                    .ERROR_MESSAGE(
                                            "Code '"
                                                + cd.code
                                                + "'[" + cd.codeSystem
                                                + "] is a duplicate of existing code & codeSystem for substance:")
                                    .addLink(ValidationUtils.createSubstanceLink(s2.toSubstanceReference()));
                        } else {
                            mes = GinasProcessingMessage
                                    .WARNING_MESSAGE(
                                            "Code '"
                                                    + cd.code
                                                    + "'[" + cd.codeSystem
                                                    + "] collides (possible duplicate) with existing code & codeSystem for substance:")
                                //                               TODO katelda Feb 2021 : add link support back!
                                    .addLink(ValidationUtils.createSubstanceLink(s2.toSubstanceReference()));
                        }
                        callback.addMessage(mes);
                    }
                }
            }
        }
    }

    public void setCodeSystemsForWarning(LinkedHashMap<Integer, String> singletonCodeSystems) {
        this.codeSystemsForWarning = singletonCodeSystems.values().stream().collect(Collectors.toSet());
    }

    public void setCodeSystemsForError(LinkedHashMap<Integer, String> strictlyUniqueCodeSystems) {
        this.codeSystemsforError = strictlyUniqueCodeSystems.values().stream().collect(Collectors.toSet());
    }
}
