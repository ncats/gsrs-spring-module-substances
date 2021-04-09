package gsrs.module.substance.services;

import ix.seqaln.SequenceIndexer;
import lombok.Builder;
import lombok.Data;
import org.jcvi.jillion.core.residue.aa.ProteinSequence;
import org.jcvi.jillion.core.residue.aa.ProteinSequenceBuilder;
import org.jcvi.jillion.core.residue.nt.NucleotideSequence;
import org.jcvi.jillion.core.residue.nt.NucleotideSequenceBuilder;
import org.jcvi.jillion.fasta.*;
import org.jcvi.jillion.testutils.NucleotideSequenceTestUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public interface SubstanceSequenceSearchService {

    @Data
    @Builder
    class SequenceSearchRequest{
        private String q;
        private SequenceIndexer.CutoffType type;
        private Double cutoff = 0.8D;
        private Integer top = 10;
        private Integer skip=0;
        private Integer fdim = 10;
        private String field;
        private String seqType = "Protein";
        /*
         Map<String, String[]> params = request().body().asFormUrlEncoded();
        String q =      getLastStringOrElse(params.get("q"), null);
        String type =   getLastStringOrElse(params.get("type"), "SUB");
        Double co =     getLastDoubleOrElse(params.get("cutoff"), 0.8);
        Integer top =   getLastIntegerOrElse(params.get("top"), 10);
        Integer skip=   getLastIntegerOrElse(params.get("skip"), 0);
        Integer fdim =  getLastIntegerOrElse(params.get("fdim"), 10);
        String field =  getLastStringOrElse(params.get("field"), "");
        String seqType = getLastStringOrElse(params.get("seqType"), "Protein");
         */

        public SanitizedSequenceSearchRequest sanitize() throws IOException {
            return new SanitizedSequenceSearchRequest(this);
        }
    }

    @Data
    class SanitizedSequenceSearchRequest{
        private String q;
        private SequenceIndexer.CutoffType type;
        private Double cutoff;
        private Integer top;
        private Integer skip;
        private Integer fdim;
        private String field;
        private String seqType;

        public SanitizedSequenceSearchRequest(SequenceSearchRequest unsanitized) throws IOException {

            this.type = unsanitized.type;
            this.cutoff = sanitizeCutOff(unsanitized.cutoff, 0.8D);
            this.top = sanitizeNumber(unsanitized.top, 10);
            this.skip = sanitizeNumber(unsanitized.top, 0);
            this.fdim = sanitizeNumber(unsanitized.top, 10);
            this.field = unsanitized.field;
            this.seqType = unsanitized.seqType ==null? "Protein" : unsanitized.seqType;

            this.q = sanitizeSequence(unsanitized.q);
        }

        private Integer sanitizeNumber(Integer i, int defaultValue) {
            if(i==null || i.intValue() <0){
                return defaultValue;
            }
            return i;

        }

        private Double sanitizeCutOff(Double i, double defaultValue) {

            if(i==null || i.intValue() <0){
                return defaultValue;
            }
            return i;
        }

        private String sanitizeSequence(String q) throws IOException {
            if(q==null){
                return q;
            }

            if(q.trim().startsWith(">")){
                //fasta record assume 1 ?
                String[] capturedSeq = new String[1];
                FastaFileParser.create(new ByteArrayInputStream(q.getBytes("utf-8")))
                        .parse(new FastaVisitor() {
                            @Override
                            public FastaRecordVisitor visitDefline(FastaVisitorCallback callback, String id, String optionalComment) {
                                return new AbstractFastaRecordVisitor(id, optionalComment) {
                                    @Override
                                    protected void visitRecord(String id, String optionalComment, String fullBody) {
                                        capturedSeq[0] = sanitizeSequenceString(fullBody);
                                    }
                                };
                            }

                            @Override
                            public void visitEnd() {

                            }

                            @Override
                            public void halted() {

                            }
                        });
                return capturedSeq[0];
            }
            return sanitizeSequenceString(q);

        }

        private String sanitizeSequenceString(String q) {
            if(isProtein()){
                return new ProteinSequenceBuilder(q).ungap().toString();
            }else{
                return new NucleotideSequenceBuilder(q).ungap().toString();
            }
        }

        boolean isProtein(){
            return "Protein".equalsIgnoreCase(seqType);
        }
    }

    Object search(SanitizedSequenceSearchRequest request) throws IOException;
}
