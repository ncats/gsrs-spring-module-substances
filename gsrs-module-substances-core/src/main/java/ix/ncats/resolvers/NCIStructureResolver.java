package ix.ncats.resolvers;

import ix.utils.Util;

import java.net.MalformedURLException;
import java.net.URL;

import org.springframework.stereotype.Component;

@Component
public class NCIStructureResolver extends AbstractStructureResolver {
    public static final String NCI_RESOLVER1 =
        "https://cactus.nci.nih.gov/chemical/structure";

    
    public NCIStructureResolver () {
        super ("NCI");
    }

    @Override
    protected UrlAndFormat[] resolvers (String name) throws MalformedURLException {
        return  new UrlAndFormat[] {
           // new AbstractStructureResolver.UrlAndFormat(new URL (NCI_RESOLVER1+"/"+Util.URLEncode(name)+"/sdf"), "sdf"),
            new UrlAndFormat(new URL (NCI_RESOLVER1+"/"+Util.URLEncode(name)+"/smiles"), "smiles")
        };
    }


}
