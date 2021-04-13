package gsrs.module.substance.substance.sequenceSearch;

import gsrs.module.substance.services.SubstanceSequenceSearchService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

public class SanitizeSequenceRequestTest {

    @Test
    public void cleanNucSequence() throws IOException {
        SubstanceSequenceSearchService.SanitizedSequenceSearchRequest sut = SubstanceSequenceSearchService.SequenceSearchRequest.builder()
                                                            .q("ACGTACGTACGTTAAA")
                                                            .seqType("NucleicAcid")
                                                            .build()
                                                            .sanitize();



        assertEquals("ACGTACGTACGTTAAA", sut.getQ());
        assertEquals("NucleicAcid", sut.getSeqType());


    }

    @Test
    public void cleanNucSequenceWithAmbiguities() throws IOException {
        SubstanceSequenceSearchService.SanitizedSequenceSearchRequest sut = SubstanceSequenceSearchService.SequenceSearchRequest.builder()
                .q("ACGNNACGTACGTTAWA")
                .seqType("NucleicAcid")
                .build()
                .sanitize();



        assertEquals("ACGNNACGTACGTTAWA", sut.getQ());
        assertEquals("NucleicAcid", sut.getSeqType());


    }
    @Test
    public void cleanNucSequenceFromFasta() throws IOException {
        SubstanceSequenceSearchService.SanitizedSequenceSearchRequest sut = SubstanceSequenceSearchService.SequenceSearchRequest.builder()
                .q(">Foo\nACGTACGT\nACGTTAAA")
                .seqType("NucleicAcid")
                .build()
                .sanitize();



        assertEquals("ACGTACGTACGTTAAA", sut.getQ());


    }
    @Test
    public void cleanNucSequenceWithWhitespace() throws IOException {
        SubstanceSequenceSearchService.SanitizedSequenceSearchRequest sut = SubstanceSequenceSearchService.SequenceSearchRequest.builder()
                .q("A  CGT\nACGTACGT\tTAAA    ")
                .seqType("NucleicAcid")
                .build()
                .sanitize();



        assertEquals("ACGTACGTACGTTAAA", sut.getQ());


    }
    @Test
    public void defaultValues() throws IOException {
        SubstanceSequenceSearchService.SanitizedSequenceSearchRequest sut = SubstanceSequenceSearchService.SequenceSearchRequest.builder()

                .build()
                .sanitize();



        assertNull( sut.getQ());
        assertEquals("Protein", sut.getSeqType());
        assertEquals(0, sut.getSkip());
        assertEquals(10, sut.getTop());
        assertEquals(10, sut.getFdim());
        assertEquals(0.8D, sut.getCutoff());


    }
    @Test
    public void negativeSkipDefaultToZero() throws IOException {
        SubstanceSequenceSearchService.SanitizedSequenceSearchRequest sut = SubstanceSequenceSearchService.SequenceSearchRequest.builder()
                .skip(-2)
                .build()
                .sanitize();


        assertEquals(0, sut.getSkip());


    }
    @Test
    public void negativeTopDefaultToTen() throws IOException {
        SubstanceSequenceSearchService.SanitizedSequenceSearchRequest sut = SubstanceSequenceSearchService.SequenceSearchRequest.builder()
                .top(-2)
                .build()
                .sanitize();


        assertEquals(10, sut.getTop());


    }
    @Test
    public void NegativeFdimDefaultToTen() throws IOException {
        SubstanceSequenceSearchService.SanitizedSequenceSearchRequest sut = SubstanceSequenceSearchService.SequenceSearchRequest.builder()
                .fdim(-2)
                .build()
                .sanitize();


        assertEquals(10, sut.getFdim());


    }
    @Test
    public void cleanNucSequenceUngap() throws IOException {
        SubstanceSequenceSearchService.SanitizedSequenceSearchRequest sut = SubstanceSequenceSearchService.SequenceSearchRequest.builder()
                .q("ACGTACGTACGT-AAA")
                .seqType("NucleicAcid")
                .build()
                .sanitize();



        assertEquals("ACGTACGTACGTAAA", sut.getQ());


    }


    @Test
    public void cleanProtSequence() throws IOException {
        SubstanceSequenceSearchService.SanitizedSequenceSearchRequest sut = SubstanceSequenceSearchService.SequenceSearchRequest.builder()
                .q("QGTLVTVSSASTKGPSVFPLAPCSRSTSESTAALG")
                .seqType("Protein")
                .build()
                .sanitize();



        assertEquals("QGTLVTVSSASTKGPSVFPLAPCSRSTSESTAALG", sut.getQ());
        assertEquals("Protein", sut.getSeqType());


    }

    @Test
    public void cleanProtSequenceWithAmbiguities() throws IOException {
        SubstanceSequenceSearchService.SanitizedSequenceSearchRequest sut = SubstanceSequenceSearchService.SequenceSearchRequest.builder()
                .q("QGTLVTVSSASTKGPSVFPLAPCSRXSTSESTAALG")
                .seqType("Protein")
                .build()
                .sanitize();



        assertEquals("QGTLVTVSSASTKGPSVFPLAPCSRXSTSESTAALG", sut.getQ());
        assertEquals("Protein", sut.getSeqType());


    }
    @Test
    public void cleanProtSequenceFromFasta() throws IOException {
        SubstanceSequenceSearchService.SanitizedSequenceSearchRequest sut = SubstanceSequenceSearchService.SequenceSearchRequest.builder()
                .q(">Foo\nQGTLVTVSSAST\nKGPSVFPLAPCSRSTSE\nSTAALG")
                .seqType("Protein")
                .build()
                .sanitize();



        assertEquals("QGTLVTVSSASTKGPSVFPLAPCSRSTSESTAALG", sut.getQ());


    }
    @Test
    public void cleanProtSequenceWithWhitespace() throws IOException {
        SubstanceSequenceSearchService.SanitizedSequenceSearchRequest sut = SubstanceSequenceSearchService.SequenceSearchRequest.builder()
                .q("  QGTLVT\tVSSASTKGPS\nVFPLAPC SRSTSESTAALG    ")
                .seqType("Protein")
                .build()
                .sanitize();



        assertEquals("QGTLVTVSSASTKGPSVFPLAPCSRSTSESTAALG", sut.getQ());


    }

    @Test
    public void cleanProtSequenceUngap() throws IOException {
        SubstanceSequenceSearchService.SanitizedSequenceSearchRequest sut = SubstanceSequenceSearchService.SequenceSearchRequest.builder()
                .q("QGTLVTVSSAST-KGPSVFPLAPCSRST-SESTAALG")
                .seqType("Protein")
                .build()
                .sanitize();



        assertEquals("QGTLVTVSSASTKGPSVFPLAPCSRSTSESTAALG", sut.getQ());


    }
}
