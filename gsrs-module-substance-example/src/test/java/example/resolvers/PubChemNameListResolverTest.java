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
    void getNameForInchiKeyTest() throws IOException, InterruptedException {
        String inChIKey = "BSYNRYMUTXBXSQ-UHFFFAOYSA-N";
        PubChemNameListResolver resolver = new PubChemNameListResolver();
        String expectedName = "2-acetyloxybenzoic acid";
        String name = resolver.getDataForChemical(inChIKey).getIupacName();
        Assertions.assertEquals(expectedName, name);
    }

    @Test
    void getCidForInchiKeyTest() throws IOException, InterruptedException {
        String smiles = "RWWYLEGWBNMMLJ-YSOARWBDSA-N";
        PubChemNameListResolver resolver = new PubChemNameListResolver();
        String expectedCid = "121304016";
        String cid = resolver.getDataForChemical(smiles).getCid();
        Assertions.assertEquals(expectedCid, cid);
    }
}
