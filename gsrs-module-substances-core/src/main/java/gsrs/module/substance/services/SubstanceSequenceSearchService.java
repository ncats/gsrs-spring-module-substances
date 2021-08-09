package gsrs.module.substance.services;

import ix.core.search.SearchResultContext;
import ix.seqaln.SequenceIndexer;
import ix.utils.Util;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jcvi.jillion.core.residue.aa.ProteinSequence;
import org.jcvi.jillion.core.residue.aa.ProteinSequenceBuilder;
import org.jcvi.jillion.core.residue.nt.NucleotideSequence;
import org.jcvi.jillion.core.residue.nt.NucleotideSequenceBuilder;
import org.jcvi.jillion.fasta.*;
import org.jcvi.jillion.testutils.NucleotideSequenceTestUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public interface SubstanceSequenceSearchService {

    enum SequenceSearchType{
        GLOBAL,
        LOCAL,
        CONTAINS
    }
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class SequenceSearchRequest{
        private String q;
        private SequenceIndexer.CutoffType type;
        private Double cutoff = 0.8D;
        private Integer top = 10;
        private Integer skip=0;
        private Integer fdim = 10;
        private String field;
        private String seqType = "Protein";
        private double identity = 0.5D;
        private SequenceSearchType searchType = SequenceSearchType.GLOBAL;
        private String order;
       

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
        private double identity;
        private String order;
        private SequenceSearchType searchType;

        public SanitizedSequenceSearchRequest(SequenceSearchRequest unsanitized) throws IOException {

            this.type = unsanitized.type == null? SequenceIndexer.CutoffType.SUB: unsanitized.type;
            this.cutoff = sanitizeCutOff(unsanitized.cutoff, 0.8D);
            this.top = sanitizeNumber(unsanitized.top, 10);
            this.skip = sanitizeNumber(unsanitized.top, 0);
            this.fdim = sanitizeNumber(unsanitized.top, 10);
            this.field = unsanitized.field;
            this.seqType = unsanitized.seqType ==null? "Protein" : unsanitized.seqType;
            this.identity = Math.min(1, Math.max(identity, 0.5D));
            this.order = unsanitized.order;
            this.searchType = unsanitized.searchType ==null? SequenceSearchType.GLOBAL: unsanitized.searchType;

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

        public boolean isProtein(){
            return "Protein".equalsIgnoreCase(seqType);
        }

        public String computeKey(){

            return Util.sha1("sequence/"+getKey (getQ() +this.searchType.name() + this.getOrder(), this.identity));

        }

        public static String getKey (String q, double t) {
            return Util.sha1(q) + "/"+String.format("%1$d", (int)(1000*t+.5));
        }

        private void putIfNotNull(String name, Object field, Map<String,String> map){
            if(field !=null){
                map.put(name, field.toString());
            }
        }
        public Map<String,String> toMap() {
            //context: String, q: String ?= null, type: String ?= "GLOBAL", cutoff: Double ?= .9, top: Int ?= 10, skip: Int ?= 0, fdim: Int ?= 10, field: String ?= "", seqType: String ?="Protein")

            Map<String,String> map = new HashMap<>();
            putIfNotNull("q", q, map);
            putIfNotNull("type", cutoff, map);
            putIfNotNull("top", top, map);
            putIfNotNull("skip", skip, map);
            putIfNotNull("field", "".equals(field)?null :field, map);
            putIfNotNull("fdim", fdim, map);
            putIfNotNull("seqType", seqType, map);
            putIfNotNull("order", order, map);
            putIfNotNull("searchType", searchType, map);
            map.put("cutoff", Double.toString(identity));
            return map;
        }
    }

    SearchResultContext search(SanitizedSequenceSearchRequest request) throws IOException;
}
