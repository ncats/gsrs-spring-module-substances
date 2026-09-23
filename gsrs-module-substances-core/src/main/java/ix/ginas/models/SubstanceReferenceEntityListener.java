package ix.ginas.models;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.springUtils.AutowireHelper;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.PostLoad;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SubstanceReference Entity Listener that on PostLoad
 * will fetch the latest version of the Substance
 * from the Repository and
 * overwrite some of the reference's fields.
 * This is because sometimes from JSON
 * the SubstanceReference in the JSON files is out of date
 * so this way we can update the entity with the current data.
 *
 * <p>From old GSRS 2.x documentation:</p>
 *
 * <p>
 * This is called after the substanceReference is loaded from the database. Here,
 * it specifically attempts to fetch any more recent version of the substance and uses
 * the information associated (approvalID, uuid, pt) with that rather than the information
 * stored on the subref itself. This is a little bit of a hack, as it would be better to
 * have this all handled automatically by the database in a very strong linked fashion.
 * The reason we can't do that right now is that there are times when the substance
 * reference is either to a substance which has not been imported yet, or may not ever
 * be imported.
 *
 * </p>
 *
 * There are 3 main use cases that stop us from changing the database setup directly:
 *
 * <ol>
 * <li>
 *    Bulk loading, where a relationship is referenced before the substance is. We need to be
 *    able to make the proper relationships more "solid" once the corresponding substance is
 *    imported. There are other ways to deal with this, like pulling the relationships outside
 *    of the object, or storing them in some temporary table until the loading is complete.
 * </li>
 * <li>
 *    Individual cherry-picked loading of substances. This is the same issue as above, but
 *    it's not coming from a bulk load but a selected one-at-a-time load. It may be possible
 *    that a related substance is never actually imported, or is imported much later.
 * </li>
 * <li>
 *    In very rare cases, this object may be used for storing a relationship to an entity which
 *    does not yet exist in any database, but will serve as a placeholder for whenever it does
 *    (if it ever does get made). This is such a rare occurrence, and it's not actually supported
 *    by the current forms. We may not want to consider this a real scenario.
 * </li>
 * </ol>
 *
 * @author katzelda
 */
@Data
public class SubstanceReferenceEntityListener {
    @Autowired
    private SubstanceRepository substanceRepository;
    /**
     * Cache of the UUIDs we are currently loading to prevent infinite loops
     * during db lookups calling PostLoad again!
     */
    private Map<UUID, Object> cache = new ConcurrentHashMap<>();
    private CachedSupplier initializer = CachedSupplier.ofInitializer(()-> AutowireHelper.getInstance().autowire(this));

    @PostLoad
    public void postLoad(SubstanceReference ref){
        initializer.get();
        UUID uuid = ref.uuid;
        //the fetch from the DB will trigger another PostLoad so cache the id so we only make 1 call
        //otherwise it will infinite loop
        if(cache.put(uuid, Boolean.TRUE) ==null) {
            //not in our cache so fetch it
            Substance fromDb = substanceRepository.findBySubstanceReference(ref);
            if (fromDb != null) {
                SubstanceReference newRef = fromDb.asSubstanceReference();
                ref.approvalID = newRef.approvalID;
                ref.refuuid = newRef.refuuid;
                ref.refPname = newRef.refPname;
            }
            cache.remove(uuid);
        }
    }

}
