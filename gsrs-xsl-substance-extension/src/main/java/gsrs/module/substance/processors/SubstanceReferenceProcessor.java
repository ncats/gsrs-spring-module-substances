package gsrs.module.substance.processors;

import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.module.substance.repository.SubstanceRepository;
import ix.core.EntityProcessor;
import ix.ginas.models.v1.SubstanceReference;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 *
 * @author Egor Puzanov
 */

@Data
@Slf4j
public class SubstanceReferenceProcessor implements EntityProcessor<SubstanceReference>{

    @Autowired
    private SubstanceRepository substanceRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private static Pattern fakeIdPattern = Pattern.compile("FAKE_ID:([0-9A-Z]{10})");
    private SubstanceReferenceProcessorConfig config;

    public static class SubstanceReferenceProcessorConfig {
        private Map<Pattern, String> codeSystemPatterns = new HashMap<Pattern, String>();
        public void setCodeSystemPatterns(Map<String, Map<String, String>> m) {
            for (Map<String, String> csp : m.values()) {
                codeSystemPatterns.put(Pattern.compile(csp.get("pattern")), csp.get("codeSystem"));
            }
        }
        public Map<Pattern, String> getCodeSystemPatterns() {
            return codeSystemPatterns;
        }
    }

    public SubstanceReferenceProcessor() {
        this(new HashMap<String, Object>());
    }

    public SubstanceReferenceProcessor(Map<String, Object> m) {
        setConfig(m);
    }

    public void setConfig(Map<String, Object> m) {
        this.config = new ObjectMapper().convertValue(m, SubstanceReferenceProcessorConfig.class);
    }

    @Override
    public void prePersist(SubstanceReference obj) throws EntityProcessor.FailProcessingException {
        SubstanceReference substanceReference = null;
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setReadOnly(true);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        if (obj.refuuid != null && !obj.refuuid.isEmpty()) {
            Matcher matcher = fakeIdPattern.matcher(obj.refuuid);
            if (matcher.find()) {
                obj.approvalID = matcher.group(1);
                obj.refuuid = UUID.randomUUID().toString();
            } else {
                substanceReference = transactionTemplate.execute(status->{
                    Optional<SubstanceRepository.SubstanceSummary> optSummary = substanceRepository.findSummaryBySubstanceReference(obj);
                    if (optSummary.isPresent()) {
                        return optSummary.get().toSubstanceReference();
                    }
                    return null;
                });
                if (substanceReference != null) {
                    return;
                }
            }
        } else {
            obj.refuuid = UUID.randomUUID().toString();
        }

        if (substanceReference == null) {
            for (Map.Entry<Pattern, String> entry : config.codeSystemPatterns.entrySet()) {
                if (obj.approvalID == null) continue;
                Matcher m = entry.getKey().matcher(obj.approvalID);
                if (m.find()) {
                    substanceReference = transactionTemplate.execute(status->{
                        Optional<SubstanceRepository.SubstanceSummary> optSummary = substanceRepository.findByCodes_CodeAndCodes_CodeSystem(obj.approvalID, entry.getValue()).stream().findFirst();
                        if (optSummary.isPresent()) {
                            return optSummary.get().toSubstanceReference();
                        }
                        return null;
                    });
                    if (substanceReference != null) {
                        break;
                    }
                }
            }
        }

        if (substanceReference == null) {
            substanceReference = transactionTemplate.execute(status->{
                Optional<SubstanceRepository.SubstanceSummary> optSummary = substanceRepository.findByNames_NameIgnoreCase(obj.refPname).stream().findFirst();
                if (optSummary.isPresent()) {
                    return optSummary.get().toSubstanceReference();
                }
                return null;
            });
        }

        if (substanceReference instanceof SubstanceReference) {
            log.debug("New SubstanceReference: " + substanceReference.toString());
            obj.refuuid = substanceReference.refuuid;
            obj.refPname = substanceReference.refPname;
            obj.approvalID = substanceReference.approvalID;
            obj.substanceClass = substanceReference.substanceClass;
        }
    }

    @Override
    public void preUpdate(SubstanceReference obj) throws EntityProcessor.FailProcessingException {
        prePersist(obj);
    }

    @Override
    public Class<SubstanceReference> getEntityClass() {
        return SubstanceReference.class;
    }
}
