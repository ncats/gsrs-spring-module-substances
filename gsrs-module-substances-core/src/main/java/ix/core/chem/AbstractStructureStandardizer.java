package ix.core.chem;

import gov.nih.ncats.molwitch.io.ChemFormat;

public abstract class AbstractStructureStandardizer implements StructureStandardizer{
    private static final int ATOM_LIMIT_FOR_STANDARDIZATION = 240;

    private final int maxNumberOfAtoms;

    public  AbstractStructureStandardizer(){
        this(ATOM_LIMIT_FOR_STANDARDIZATION);
    }

    public AbstractStructureStandardizer(int maxNumberOfAtoms){
        this.maxNumberOfAtoms = maxNumberOfAtoms;
    }

    public int getMaxNumberOfAtoms() {
        return maxNumberOfAtoms;
    }
}
