package ix.ginas.models;

/**
 * Created by mandavag on 10/24/16.
 */
public interface GinasSubstanceDefinitionAccess {

    
    /**
     * Return the part of the substance that represents what makes it different from other substances of the same class
     * @return the section of the object that contains the defining feature(s)
     */
    GinasAccessReferenceControlled getDefinitionElement();
}
