package ix.core.chem;

import gov.nih.ncats.molwitch.Atom;
import gov.nih.ncats.molwitch.Bond;
import gov.nih.ncats.molwitch.Chemical;
import ix.core.models.Structure;
import ix.core.util.LogUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Chemistry utilities
 */
@Slf4j
public class Chem {
    private Chem () {}

    public static final String WILDCARD_SUBSTITUTION_ATOM = "He";
    public static final Integer WILDCARD_SUBSTITUTION_ATOM_NUMBER = 2;

    public static void setFormula (Structure struc) {
        try {
            struc.formula = formula (struc.toChemical(false));
        }
        catch (Exception ex) {
            LogUtil.error(()->"error computing formula for structure", ex);
        }
    }
    
    public static Chemical RemoveQueryFeaturesForPseudoInChI(Chemical c) {
        Chemical chemicalToUse = c;
        if(c.hasQueryAtoms() || c.atoms().filter(at->("A".equals(at.getSymbol()) || "*".equals(at.getSymbol()) || "R".equals(at.getSymbol()))).count()>0){
            chemicalToUse = c.copy();
            chemicalToUse.atoms()
                    .filter(at-> at.getSymbol() == null || "A".equals(at.getSymbol()) || "*".equals(at.getSymbol())
                            || "R".equals(at.getSymbol()))//isQueryAtom returns true
                    .forEach(a->{
                        a.setAtomicNumber(WILDCARD_SUBSTITUTION_ATOM_NUMBER);
                        a.setAlias(WILDCARD_SUBSTITUTION_ATOM);
                        a.setMassNumber(6);
                    });
        }
        Chemical processBonds = chemicalToUse.copy();
        try{
            Chemical finalChem= Chemical.parse(ChemCleaner.removeSGroupsAndLegacyAtomLists(processBonds.toMol()));
            finalChem.bonds().filter(b->b.getBondType() == null || b.getBondType().equals(Bond.BondType.SINGLE_OR_DOUBLE) || b.isQueryBond())
                    .forEach(b->{
                        log.trace("about to replace bond {}", b);
                        b.setBondType(Bond.BondType.SINGLE);
                    });
            return finalChem;
        }catch(Exception e){
            return processBonds;
        }
    }


    public static Chemical fixMetals(Chemical chemical){
        for(Atom atom : chemical.getAtoms()){
            if(!atom.isQueryAtom() && !atom.isPseudoAtom() && atom.isMetal()){
                atom.setImplicitHCount(0);
            }
    	}

        return chemical;
    }


    /**
     * Generate chemical formula by treating disconnected components
     * separately.
     */
    public static String formula (Chemical g) {
        Iterator<Chemical> iter = g.copy().connectedComponents();
        final Map<String, AtomicInteger> formula = new HashMap<>();
        while(iter.hasNext()){
            Chemical m = iter.next();
//            if(m.hasPseudoAtoms() || m.hasQueryAtoms()){
//                continue;
//            }
        	fixMetals(m);
            String f = FormulaInfo.toCanonicalString(m.getFormula());
            formula.computeIfAbsent(f, new Function<String, AtomicInteger>() {
                @Override
                public AtomicInteger apply(String s) {
                    return new AtomicInteger();
        }
            }).incrementAndGet();
        }
        StringBuilder sb = new StringBuilder ();
        for(Map.Entry<String, AtomicInteger> entry : formula.entrySet()){
            int c =entry.getValue().get();
            if(sb.length() > 0){
                sb.append('.');
        }
            if(c > 1) {
                sb.append(c);
            }
            sb.append(entry.getKey());
        }

        return FormulaInfo.toCanonicalString(sb.toString());
    }
    
    /**
     * Returns true if there is a problem exporting this chemical object
     * @param c
     * @return
     */
	public static boolean isProblem(Chemical c){
		boolean problem = false;
		
		try{
            String o=c.toSd();
		}catch(Exception e){
			log.warn("Error exporting molecule", e);
			problem=true;
		}
		return problem;
	}

}
