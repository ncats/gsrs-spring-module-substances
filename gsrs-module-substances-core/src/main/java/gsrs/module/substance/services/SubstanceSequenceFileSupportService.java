package gsrs.module.substance.services;

import gov.nih.ncats.common.sneak.Sneak;
import gsrs.repository.PayloadRepository;
import gsrs.sequence.SequenceFileSupport;
import gsrs.service.PayloadService;
import ix.core.models.Payload;
import ix.core.models.SequenceEntity;
import ix.ginas.models.v1.Substance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class SubstanceSequenceFileSupportService {

    private static Pattern PAYLOAD_UUID_PATTERN = Pattern.compile("payload\\((.+?)\\)");

    private final PayloadService payloadService;
    private final PayloadRepository payloadRepository;

    private final PlatformTransactionManager transactionManager;

    @Autowired
    public SubstanceSequenceFileSupportService(PayloadService payloadService,
                                               PayloadRepository payloadRepository,
                                               PlatformTransactionManager transactionManager) {
        this.payloadService = payloadService;
        this.payloadRepository = payloadRepository;
        this.transactionManager = transactionManager;
    }

    public boolean hasSequenceFiles(Substance s){
        return getFastaPayloadIdsFor(s).findAny().isPresent();
    }

    private Stream<String> getFastaPayloadIdsFor(Substance s){
        return s.references.stream()
                .filter(r -> r.uploadedFile != null)
                .flatMap(r -> {
                    if (r.tags.stream()
//											.peek(k -> System.out.println(k))
                            .filter(k -> k.term.equalsIgnoreCase("fasta"))
                            .findAny()
                            .isPresent()) {
                        Matcher m = PAYLOAD_UUID_PATTERN.matcher(r.uploadedFile);
                        if (m.find()) {
                            return Stream.of(m.group(1));
                        }
                    }
                        return Stream.empty();
                    });
    }


    public <T extends Substance> Stream<SequenceFileSupport.SequenceFileData> getSequenceFileDataFor(T sequenceSubstance, SequenceEntity.SequenceType sequenceType){
        return getFastaPayloadIdsFor(sequenceSubstance)
                //TODO: hardcode fasta for now we can add more file types if we ever add support for others
                .map(id-> new PayloadBackedSequenceFileData(sequenceType,
                                                        SequenceFileSupport.SequenceFileData.SequenceFileType.FASTA,
                                                        UUID.fromString(id)));
    }

    private class PayloadBackedSequenceFileData implements SequenceFileSupport.SequenceFileData{
        private final SequenceEntity.SequenceType type;
        private final SequenceFileType getSequenceFileType;
        private final UUID payloadUUID;

        public PayloadBackedSequenceFileData(SequenceEntity.SequenceType type, SequenceFileType getSequenceFileType, UUID payloadUUID) {
            this.type = type;
            this.payloadUUID = payloadUUID;
            this.getSequenceFileType = getSequenceFileType;
        }

        @Override
        public SequenceEntity.SequenceType getSequenceType() {
            return type;
        }

        @Override
        public SequenceFileType getSequenceFileType() {

            return getSequenceFileType;
        }

        @Override
        public InputStream createInputStream() throws IOException {
            //we need to make sure we're in a transaction
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            Optional<InputStream> in= tx.execute( ignored -> {try {
                return payloadService.getPayloadAsInputStream(payloadUUID);
            }catch(IOException e){
                return Sneak.sneakyThrow(e);
            }});
            if(in.isPresent()){
                return in.get();
            }
            throw new IOException("could not find inputstream for payload " + payloadUUID);
        }

        @Override
        public String getName() {
            Optional<Payload> payload= payloadRepository.findById(payloadUUID);
            if(!payload.isPresent()) {
                return null;
            }
            return payload.get().name;
        }
    }

}
