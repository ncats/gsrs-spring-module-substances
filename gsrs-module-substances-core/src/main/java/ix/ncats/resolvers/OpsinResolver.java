package ix.ncats.resolvers;

import ix.core.models.Structure;
import uk.ac.cam.ch.wwmm.opsin.NameToStructure;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;

import org.springframework.stereotype.Component;

/**
 * Created by katzelda on 3/29/18.
 */
@Component
public class OpsinResolver extends AbstractStructureResolver{

    private static final NameToStructure nts =  NameToStructure.getInstance();
    public OpsinResolver() {
        super("OPSIN");
    }

    @Override
    protected UrlAndFormat[] resolvers(String name) throws MalformedURLException {
        return new UrlAndFormat[0]; // unused
    }

    @Override
    public Structure resolve(String name) {
    	try{
    		//This shouldn't be needed, but for some reason it comes in URLEncoded

        String smiles = nts.parseToSmiles(name);

        if(smiles !=null){
            try {
                return this.resolve(new ByteArrayInputStream(smiles.getBytes()), "smiles");
            } catch (IOException e) {
                return null;
            }

	        }
    	}catch(Exception e){
    		e.printStackTrace();

        }
        return null;
    }
}
