package example.resolvers;

import ix.ncats.resolvers.PubChemNameListResolver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class PubChemNameListResolverTest {

    @Test
    void getIupacNameForCid() throws IOException, InterruptedException {
        String cid = "3226";
        PubChemNameListResolver resolver = new PubChemNameListResolver();
        String iupacName = resolver.getIupacNameForCid(cid);
        String expectedName = "2-chloro-1-(difluoromethoxy)-1,1,2-trifluoroethane";
        System.out.printf("received name '%s' for CID %s %n", iupacName, cid);
        Assertions.assertEquals(expectedName, iupacName);
    }

    @Test
    void getNameForSmilesTest() throws IOException, InterruptedException {
        String smiles = "c1cccc(OC(C)=O)c1C(=O)O";
        PubChemNameListResolver resolver = new PubChemNameListResolver();
        String expectedName = "2-acetyloxybenzoic acid";
        String name = resolver.getNamesData(smiles);
        Assertions.assertEquals(expectedName, name);
    }
}
