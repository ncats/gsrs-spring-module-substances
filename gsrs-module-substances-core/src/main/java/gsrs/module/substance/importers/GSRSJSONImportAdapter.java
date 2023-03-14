package gsrs.module.substance.importers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.imports.ImportAdapter;
import gsrs.json.JsonEntityUtil;
import gsrs.module.substance.services.SubstanceBulkLoadService;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.JsonSubstanceFactory;
import java.io.InputStream;
import java.util.stream.Stream;

public class GSRSJSONImportAdapter implements ImportAdapter<Substance> {

    @Override
    public Stream<Substance> parse(InputStream is, ObjectNode settings) {
        Stream.Builder<Substance> newSubstanceStream= Stream.builder();
        SubstanceBulkLoadService.GinasDumpExtractor dumpExtractor = new SubstanceBulkLoadService.GinasDumpExtractor(is);
        try {
            JsonNode currentRecord = dumpExtractor.getNextRecord();
            while(currentRecord!= null) {
                Substance converted = convertJsonNode(currentRecord);
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
