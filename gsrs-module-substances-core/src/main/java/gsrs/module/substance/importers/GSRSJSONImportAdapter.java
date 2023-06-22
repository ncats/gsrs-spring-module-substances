package gsrs.module.substance.importers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.imports.ImportAdapter;
import gsrs.json.JsonEntityUtil;
import gsrs.module.substance.services.SubstanceBulkLoadService;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.JsonSubstanceFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.InputStream;
import java.util.stream.Stream;

@Slf4j
public class GSRSJSONImportAdapter implements ImportAdapter<Substance> {

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Override
    public Stream<Substance> parse(InputStream is, ObjectNode settings, JsonNode schema) {
        Stream.Builder<Substance> newSubstanceStream= Stream.builder();
        SubstanceBulkLoadService.GinasDumpExtractor dumpExtractor = new SubstanceBulkLoadService.GinasDumpExtractor(is);
        try {
            JsonNode currentRecord = dumpExtractor.getNextRecord();
            while(currentRecord!= null) {

                log.trace("About to convert JSON to substance");
                JsonNode finalCurrentRecord= currentRecord;
                TransactionTemplate txManageConversion = new TransactionTemplate(platformTransactionManager);
                txManageConversion.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                Substance converted=txManageConversion.execute(t-> convertJsonNode(finalCurrentRecord));

                log.trace("converted JSON to substance with ID {}.  It has {} names", converted.getUuid(), converted.names.size());
                JsonNode jsonNode= converted.toFullJsonNode();
                log.trace("converted to a JSON node of type {}", jsonNode.getNodeType());
                newSubstanceStream.add(converted);
                currentRecord = dumpExtractor.getNextRecord();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return newSubstanceStream.build();
    }

    private Substance convertJsonNode(JsonNode node){
        Substance substance= JsonSubstanceFactory.makeSubstance(node);
        return JsonEntityUtil.fixOwners(substance, true);
    }
}
