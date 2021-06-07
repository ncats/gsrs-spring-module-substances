package ix.ncats.resolvers;

import gov.nih.ncats.common.util.TimeUtil;
import ix.core.models.Structure;

/**
 *
 * {@link PubChemStructureResolver} wrapper since
 * PubChem requires a 5 sec wait between requests.
 *
 * Created by katzelda on 4/10/18.
 */
public class PubChemService implements Resolver<Structure>{



    private static final int MIN_WAIT_TIME = 5_000;
    private long lastRun=0;

    private final PubChemStructureResolver resolver = new PubChemStructureResolver();

    public synchronized Structure resolve(String name) {
        long currentTime = TimeUtil.getCurrentTimeMillis();
        long delta = currentTime - lastRun;
        lastRun = currentTime;

        if(delta < MIN_WAIT_TIME){
            try {
                Thread.sleep(MIN_WAIT_TIME- delta);
            } catch (InterruptedException e) {
                return null;
            }
        }

        return resolver.resolve(name);


    }


    @Override
    public Class<Structure> getType() {
        return resolver.getType();
    }

    @Override
    public String getName() {
        return resolver.getName();
    }

}
