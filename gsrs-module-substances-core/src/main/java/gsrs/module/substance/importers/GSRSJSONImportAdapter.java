package gsrs.module.substance.importers;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
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
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
public class GSRSJSONImportAdapter implements ImportAdapter<Substance> {

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    private final LongAdder getNextRecordNanos = new LongAdder();
    private final LongAdder convertNanos = new LongAdder();

    @Override
    public Stream<Substance> parse(
            InputStream is,
            ObjectNode settings,
            JsonNode schema) {

        SubstanceBulkLoadService.GinasDumpExtractor dumpExtractor =
                new SubstanceBulkLoadService.GinasDumpExtractor(is);

        Spliterator<Substance> spliterator =
                new Spliterators.AbstractSpliterator<>(
                        Long.MAX_VALUE,
                        Spliterator.ORDERED | Spliterator.NONNULL) {

                    private boolean finished;

                    @Override
                    public boolean tryAdvance(Consumer<? super Substance> action) {
                        if (finished) {
                            return false;
                        }

                        try {
                            long startNanos = System.nanoTime();
                            JsonNode record;
                            try {
                                record=dumpExtractor.getNextRecord();
                            } finally {
                                getNextRecordNanos.add(System.nanoTime() - startNanos);
                            }

                            if (record == null) {
                                finish();
                                return false;
                            }

                            TransactionTemplate transaction =
                                    new TransactionTemplate(platformTransactionManager);

                            transaction.setPropagationBehavior(
                                    TransactionDefinition.PROPAGATION_REQUIRES_NEW);

                            Substance substance = transaction.execute(
                                    status -> {
                                        long beforeConvert = System.nanoTime();
                                        try {
                                            return convertJsonNode(record);
                                        } finally {
                                            convertNanos.add(System.nanoTime() - beforeConvert);
                                        }
                                    });

                            if (substance == null) {
                                throw new IllegalStateException(
                                        "JSON record conversion returned null");
                            }

                            action.accept(substance);
                            return true;
                        } catch (Exception e) {
                            finish();
                            throw new RuntimeException(
                                    "Error reading or converting a GSRS JSON record", e);
                        }
                    }

                    private void finish() {
                        if (!finished) {
                            finished = true;
                            dumpExtractor.close();
                        }
                    }
                };

        return StreamSupport.stream(spliterator, false)
                .onClose(dumpExtractor::close);
    }

    private Substance convertJsonNode(JsonNode node){
        Substance substance= JsonSubstanceFactory.makeSubstance(node);
        return JsonEntityUtil.fixOwners(substance, true);
    }
}
