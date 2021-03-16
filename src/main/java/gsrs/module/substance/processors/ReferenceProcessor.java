package gsrs.module.substance.processors;

import gsrs.EntityProcessorFactory;
import ix.core.EntityProcessor;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Relationship;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

/**
 * Created by katzelda on 12/2/18.
 */
public class ReferenceProcessor implements EntityProcessor<Reference> {

    @Autowired
    private EntityProcessorFactory entityProcessorFactory;

    public ReferenceProcessor() {
    }

    public EntityProcessorFactory getEntityProcessorFactory() {
        return entityProcessorFactory;
    }

    public void setEntityProcessorFactory(EntityProcessorFactory entityProcessorFactory) {
        this.entityProcessorFactory = entityProcessorFactory;
    }

    @Override
    public Class<Reference> getEntityClass() {
        return Reference.class;
    }

    @Override
    public void preUpdate(Reference obj) throws FailProcessingException {
//        System.out.println("updating reference");

        //Relationships often have an inverted relationship which
        //will have its own references but which are usually the
        // from an end user perspective, the same
        //but from the object model are 2 separate entities that happen to have
        //the same data.
        //so we need to keep them in sync.  The Relationship processor deals with that
        //but might not get fired if the reference is the only thing on the relationship
        //that changes.
        //so we manually fire the relationship processor on all affected relationships

        for(GinasAccessReferenceControlled referred : obj.getElementsReferencing()){
//            System.out.println("referred = " + referred);
            if(referred instanceof Relationship){
                entityProcessorFactory.getCombinedEntityProcessorFor(referred).preUpdate(referred);

            }
        }


    }

    @Override
    public void prePersist(Reference obj) throws FailProcessingException {
        //don't think we need to bother with pre -persist whatever
        //creates this reference should handle propagation and copying
//        System.out.println("prepersist reference");
    }

    @Override
    public void preRemove(Reference obj) throws FailProcessingException {
//        System.out.println("pre remove " + obj);
        //if it's been removed is it still in the getElements refercing ?
        //I don't think so...
        //GinasAccessReferenceControlled only link to the UUID of this reference
        //so removing a reference from that list without removing it from the substance
        //shouldn't fire this processor
        //and the validation shouldn't let a substance remove the reference
        //without also removing all the UUIDs in the corresponding GinasAccessReferenceControlled objects
        //so maybe we don't need this either...
        for(GinasAccessReferenceControlled referred : obj.getElementsReferencing()){
            System.out.println("still referred by " + referred);

        }
    }
}
