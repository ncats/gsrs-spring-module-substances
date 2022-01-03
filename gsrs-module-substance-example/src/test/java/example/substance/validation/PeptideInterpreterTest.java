package example.substance.validation;

import ix.ginas.utils.validation.PeptideInterpreter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

/**
 * Created by katzelda on 7/15/19.
 */
public class PeptideInterpreterTest{

    private static Stream<Arguments> peptideData() {
        return Stream.of(
                Arguments.of("CC[C@H](C)[C@@H]1NC(=O)[C@H](CC2=CC=C(O)C=C2)NC(=O)[C@@H](N)CSSC[C@H](NC(=O)[C@H](CC(N)=O)NC(=O)[C@H](CCC(N)=O)NC1=O)C(=O)N3CCCC3C(=O)N[C@@H](CC(C)C)C(=O)NCC(N)=O"
                ,"CYIQNCPLG"),

                Arguments.of("CC[C@H](C)[C@@H](C(=O)N[C@@H](CCC(=O)O)C(=O)N[C@@H](CC1=CNC2=CC=CC=C21)C(=O)N[C@@H](CC(C)C)C(=O)N[C@@H](CCCCN)C(=O)N[C@@H](CC(=O)N)C(=O)NCC(=O)NCC(=O)N3CCC[C@H]3C(=O)N[C@@H](CO)C(=O)N[C@@H](CO)C(=O)NCC(=O)N[C@@H](C)C(=O)N4CCC[C@H]4C(=O)N5CCC[C@H]5C(=O)N6CCC[C@H]6C(=O)N[C@@H](CO)C(=O)N)NC(=O)[C@H](CC7=CC=CC=C7)NC(=O)[C@H](CC(C)C)NC(=O)[C@H](CCCNC(=N)N)NC(=O)[C@H](C(C)C)NC(=O)[C@H](C)NC(=O)[C@H](CCC(=O)O)NC(=O)[C@H](CCC(=O)O)NC(=O)[C@H](CCC(=O)O)NC(=O)[C@H](CCSC)NC(=O)[C@H](CCC(=O)N)NC(=O)[C@H](CCCCN)NC(=O)[C@H](CO)NC(=O)[C@H](CC(C)C)NC(=O)[C@H](CC(=O)O)NC(=O)[C@H](CO)NC(=O)[C@H]([C@@H](C)O)NC(=O)[C@H](CC8=CC=CC=C8)NC(=O)[C@H]([C@@H](C)O)NC(=O)CNC(=O)[C@H](CCC(=O)O)NC(=O)CNC(=O)[C@H](CC9=CN=CN9)N"
                ,"HGEGTFTSDLSKQMEEEAVRLFIEWLKNGGPSSGAPPPS"),
                Arguments.of("CCCCCCCCCCCCCCCC(=O)N[C@@H](CCC(=O)O)C(=O)NCCCC[C@@H](C(=O)N[C@@H](CCC(=O)O)C(=O)N[C@@H](Cc1ccccc1)C(=O)N[C@@H]([C@@H](C)CC)C(=O)N[C@@H](C)C(=O)N[C@@H](Cc2c[nH]c3c2cccc3)C(=O)N[C@@H](CC(C)C)C(=O)N[C@@H](C(C)C)C(=O)N[C@@H](CCCNC(=N)N)C(=O)NCC(=O)N[C@@H](CCCNC(=N)N)C(=O)NCC(=O)O)NC(=O)[C@H](C)NC(=O)[C@H](C)NC(=O)[C@H](CCC(=O)N)NC(=O)CNC(=O)[C@H](CCC(=O)O)NC(=O)[C@H](CC(C)C)NC(=O)[C@H](Cc4ccc(cc4)O)NC(=O)[C@H](CO)NC(=O)[C@H](CO)NC(=O)[C@H](C(C)C)NC(=O)[C@H](CC(=O)O)NC(=O)[C@H](CO)NC(=O)[C@H]([C@@H](C)O)NC(=O)[C@H](Cc5ccccc5)NC(=O)[C@H]([C@@H](C)O)NC(=O)CNC(=O)[C@H](CCC(=O)O)NC(=O)[C@H](C)NC(=O)[C@H](Cc6cnc[nH]6)N"
                ,"HAEGTFTSDVSSYLEGQAAXEFIAWLVRGRG"),
                Arguments.of("CC[C@H](C)[C@@H](C(=O)N[C@@H](CCC(=O)O)C(=O)N[C@@H](CC1=CNC2=CC=CC=C21)C(=O)N[C@@H](CC(C)C)C(=O)N[C@@H](CCCCN)C(=O)N[C@@H](CC(=O)N)C(=O)NCC(=O)NCC(=O)N3CCC[C@H]3C(=O)N[C@@H](CO)C(=O)N[C@@H](CO)C(=O)NCC(=O)N[C@@H](C)C(=O)N4CCC[C@H]4C(=O)N5CCC[C@H]5C(=O)N[C@@H](CO)C(=O)N[C@@H](CCCCN)C(=O)N[C@@H](CCCCN)C(=O)N[C@@H](CCCCN)C(=O)N[C@@H](CCCCN)C(=O)N[C@@H](CCCCN)C(=O)N[C@@H](CCCCN)C(=O)N)NC(=O)[C@H](CC6=CC=CC=C6)NC(=O)[C@H](CC(C)C)NC(=O)[C@H](CCCNC(=N)N)NC(=O)[C@H](C(C)C)NC(=O)[C@H](C)NC(=O)[C@H](CCC(=O)O)NC(=O)[C@H](CCC(=O)O)NC(=O)[C@H](CCC(=O)O)NC(=O)[C@H](CCSC)NC(=O)[C@H](CCC(=O)N)NC(=O)[C@H](CCCCN)NC(=O)[C@H](CO)NC(=O)[C@H](CC(C)C)NC(=O)[C@H](CC(=O)O)NC(=O)[C@H](CO)NC(=O)[C@H]([C@@H](C)O)NC(=O)[C@H](CC7=CC=CC=C7)NC(=O)[C@H]([C@@H](C)O)NC(=O)CNC(=O)[C@H](CCC(=O)O)NC(=O)CNC(=O)[C@H](CC8=CN=CN8)N",
                "HGEGTFTSDLSKQMEEEAVRLFIEWLKNGGPSSGAPPSKKKKKK")
        );
    }


    @ParameterizedTest
    @MethodSource("peptideData")
    public void assertSequenceIntepretedCorrectly(String smiles, String expectedSequence) throws Exception{
        PeptideInterpreter.Protein p = PeptideInterpreter.getAminoAcidSequence(smiles);
        
        List<PeptideInterpreter.Protein.Subunit> subunits = p.getSubunits();
        assertEquals(1, subunits.size());
        String t=subunits.get(0).getSequence();
        // Whether to include NH2 on C-term as X or not is not a big deal,
        // and should be allowed some flexibility
        if(t.endsWith("X")) {
            t=t.substring(0,t.length()-1);
        }
        assertEquals(expectedSequence, t);
    }
}
