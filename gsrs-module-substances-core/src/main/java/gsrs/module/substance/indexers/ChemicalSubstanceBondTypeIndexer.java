package gsrs.module.substance.indexers;

import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.nih.ncats.molwitch.Bond;
import gov.nih.ncats.molwitch.Chemical;
import ix.core.chem.StructureProcessor;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.core.search.text.ReflectingIndexValueMaker;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

/**
 * Adds chemical bond types as a facet for quick filtering. 
 * 
 */
@Component
@Slf4j
public class ChemicalSubstanceBondTypeIndexer implements IndexValueMaker<Substance>{
    private static final String TEXT_SINGLE_BOND = "Single Bond";
    private static final String TEXT_DOUBLE_BOND = "Double Bond";
    private static final String TEXT_TRIPLE_BOND = "Triple Bond";
    private static final String TEXT_AROMATIC_BOND = "Aromatic Bond";
    private static final String CHEMICAL_BONDS_FACET_NAME = "Chemical Bonds";

    
    private static final boolean SMILES_LIKE_SYNTAX = false;


    @Override
    public Class<Substance> getIndexedEntityClass() {
        return Substance.class;
    }

    //This is the method which does the work
    @Override
    public void createIndexableValues(Substance s, Consumer<IndexableValue> consumer) {
        if(s instanceof ChemicalSubstance){
            addBondTypes((ChemicalSubstance)s, consumer);
        }
    }

    /**
     * Create a canonical text string for the supplied {@link Bond}. This returns
     * the atoms in pseudo hill-order, where carbon is always first, hydrogen is always
     * last, and the other atoms are sorted alphabetically.
     *  
     * @param b - The chemical bond to be canonicalized
     * @return A canonical string for the bond
     */
    private String stdBond(Bond b) {
        String typeText= TEXT_SINGLE_BOND;

        String a1=b.getAtom1().getSymbol();
        String a2=b.getAtom2().getSymbol();
        if(a1.equals("C")) {

        }else if(a2.equals("C")) {
            String t=a1;
            a1=a2;
            a2=t;
        }else if(a1.equals("H")) {
            String t=a1;
            a1=a2;
            a2=t;
        }else if(a2.equals("H")) {

        }else if(a1.compareTo(a2)>0) {
            String t=a1;
            a1=a2;
            a2=t;
        }

        String bt="-";
        switch(b.getBondType().getOrder()) {
        case 2:
            bt="=";
            typeText=TEXT_DOUBLE_BOND;
            break;
        case 3:
            bt="#";
            typeText=TEXT_TRIPLE_BOND;
            break;
        default:
            bt="-";
            typeText=TEXT_SINGLE_BOND;
            break;
        }
        if(b.isAromatic()) {
            bt=":";
            typeText=TEXT_AROMATIC_BOND;
        }


        //This will return in smiles-like syntax
        if(SMILES_LIKE_SYNTAX) {
            return a1 + bt + a2;
        }else {
            //this returns in more verbose syntax
            return a1 + "-" + a2 + ", " + typeText;    
        }
    }

    
    /**
     * Indexes all the unique bonds found in the supplied {@link ChemicalSubstance} by producing
     * the canonical bond names using {@link #stdBond(Bond)}. This method also adds explicit hydrogens
     * and aromatizes the structure (transiently) before indexing.
     * @param s - The {@link ChemicalSubstance} to be indexed
     * @param consumer - The consumer to accept new {@link IndexableValue} objects
     */
    public void addBondTypes(ChemicalSubstance s, Consumer<IndexableValue> consumer) {
        try{
            //Quick simple standardization of the supplied molecule
            Chemical c = s.toChemical();

            //We want hydrogens explicitly populated, this adds them
            c.makeHydrogensExplicit();

            //Aromatic bonds should be present, not kekulized bonds
            c.aromatize();

            //for each bond, get canonical representation, sort, get unique forms, add to index
            c.bonds().map(b->stdBond(b))
            .sorted()
            .distinct()
            .forEach(bt->{
                consumer.accept(IndexableValue.simpleFacetStringValue(CHEMICAL_BONDS_FACET_NAME, bt));
            });
        }catch(Exception e){
            log.warn("Problem indexing substance for bond types:" + s.uuid, e);
        }
    }

}
