package ix.core.chem;

import gov.nih.ncats.common.util.CachedSupplier;
import gov.nih.ncats.molwitch.*;
import gov.nih.ncats.molwitch.spi.ChemicalImplFactory;
import ix.core.models.Keyword;
import ix.core.models.Structure;
import ix.core.models.Text;
import ix.core.models.Value;
import ix.ginas.utils.ChemUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static gov.nih.ncats.molwitch.spi.ChemicalImplFactory.applyParameters;

@Slf4j
@Component
@Configuration
@ConfigurationProperties("gsrs.structure.processing")
public class StructureProcessor {


    private StructureStandardizer standardizer;

    private StructureHasher hasher;

    private Integer complexityCutoff;

    private Integer maxUndefined;

    private List<String> processedFactoryClasses =  new ArrayList<>();

    @Autowired
    public StructureProcessor(StructureStandardizer standardizer, StructureHasher hasher) {
        this.standardizer = standardizer;
        this.hasher = hasher;
    }

    public Structure instrument (byte[] buf) {
        return instrument (buf, null);
    }

    public Structure instrument (byte[] buf,
                                        Collection<Structure> components) {
        return instrument (buf, components, true);
    }

    public Structure instrument (byte[] buf,
                                        Collection<Structure> components,
                                        boolean standardize) {
        try {
            return instrument (Chemical.parse(new String(buf)),
                    components, standardize);
        }
        catch (Exception ex) {
            throw new IllegalArgumentException (ex);
        }
    }

    public Structure instrument (String mol) {
        return instrument (mol, null, true);
    }

    public Structure instrument
            (String mol, Collection<Structure> components) {
        return instrument (mol, components, true);
    }

    public StructureProcessorTask.Builder taskFor(String mol) throws Exception {
        Structure struc = new Structure();
        struc.digest = digest (mol);
        try {
            return new StructureProcessorTask.Builder()
                    .structure(struc)
                    .mol(mol)
                    .processor(this);
            
        }catch(Exception e) {
            String nmol = ChemCleaner.removeSGroupsAndLegacyAtomLists(mol);
            return new StructureProcessorTask.Builder()
                    .structure(struc)
                    .mol(nmol)
                    .processor(this);
        }
    }
    public StructureProcessorTask.Builder taskFor(String mol, Collection<Structure> components, boolean standardize) throws Exception {
        return taskFor(mol)
                .components(components)
                .standardize(standardize);
    }
    
    public Structure instrument
            (String mol, Collection<Structure> components, boolean standardize) {
        try {
            StructureProcessorTask task = taskFor(mol, components, standardize).build();
            task.instrument();
            return task.getStructure();
        }catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Structure instrument (Chemical mol) {
        return instrument (mol, true);
    }

    public Structure instrument (Chemical mol, boolean standardize) {
        return instrument (mol, null, standardize);
    }

    public Structure instrument (Chemical mol,
                                        Collection<Structure> components) {
        return instrument (mol, components, true);
    }

    public Structure instrument (Chemical mol,
                                        Collection<Structure> components,
                                        boolean standardize) {
        Structure struc = new Structure();
        instrument (struc, components, mol, standardize);
        return struc;
    }

    void instrument (Structure struc,
                            Collection<Structure> components,
                            Chemical mol) {
        instrument (struc, components, mol, true);
    }

    /**
     * All instrument calls lead to this one
     * @param settings
     */
    void instrument (StructureProcessorTask settings) {
        Structure struc = settings.getStructure();
        Collection<Structure> components = settings.getComponents();
        Chemical mol = settings.getChemical().copy();
        if( maxUndefined !=null || complexityCutoff !=null ){
            Class factoryClass = mol.getImpl().getFactoryClass();
            if( !processedFactoryClasses.contains(factoryClass.getName())) {
                log.trace("applying ChemicalImpl parameters.  maxUndefined: {} complexityCutoff: {}", maxUndefined, complexityCutoff);
                Map<String, Object> parameters = new HashMap<>();
                if (maxUndefined != null) {
                    parameters.put("maxUndefined", maxUndefined);
                }
                if (complexityCutoff != null) {
                    parameters.put("complexityCutoff", complexityCutoff);
                }
                log.trace("about to call ChemicalImplFactory.applyParameters using reflection");
                try {
                    Method applyParameterMethod = factoryClass.getDeclaredMethod("applyParameters", Map.class);
                    applyParameterMethod.invoke(factoryClass, parameters);
                } catch (Exception ex) {
                    log.error("Error calling method on class {}", factoryClass.getName(), ex);
                }
                processedFactoryClasses.add(factoryClass.getName());
            } else { log.trace("class {} has already been set-up", factoryClass.getName());}
        } else {
            log.trace("no parameters. mol is {}", mol.getClass().getName());
        }

        boolean standardize = settings.isStandardize();
        boolean query = settings.isQuery();

        if(mol.hasQueryAtoms() || mol.hasPseudoAtoms()) {
            
            query=true; 
        }

        CachedSupplier<String> molSupplier = CachedSupplier.of(new Supplier<String>() {
            public String get(){
                try {
                    return mol.toMol();
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });
        if (struc.digest == null) {
            struc.digest = digest (molSupplier.get());

        }
        //katzelda this probably isn't needed anymore since now settings.getChemical should
        //compute coords if needed ??
        if (!mol.hasCoordinates()) {
            try {
                mol.generateCoordinates();
                molSupplier.resetCache();
            } catch (MolwitchException e) {
                e.printStackTrace();
            }
        }

        if(query){
            struc.molfile = molSupplier.get();
            try {
            	try{
            		struc.smiles = mol.toSmiles();
            	}catch(Exception e2){
            	    struc.smiles = mol.toSmarts();
            	}
            } catch (Exception e) {
//                e.printStackTrace();
            }
        }

        // no explicit Hs
        //dkatzel aug 2019 - except when the Hs are stereo and explicitly drawn that way...
        mol.removeNonDescriptHydrogens();

        // make sure molecule is kekulized consistently
        try{
        mol.kekulize();
        }catch(Exception e){
        	//it's okay to fail to kekulize in rare cases, particularly
        	//in some delocalized structures.
        }
        molSupplier.resetCache();
        if(!query){

            
            struc.molfile = molSupplier.get();
        }

        Chem.fixMetals(mol);
        molSupplier.resetCache();

        int stereo = 0, def = 0, charge = 0, ez=0;

        for(Atom a : mol.getAtoms()){
            charge += a.getCharge();
        }

        List<Stereocenter> stereocenters = new ArrayList<>(mol.getTetrahedrals());
        for(Stereocenter stereocenter : stereocenters) {
            stereo++;
            if(stereocenter.isDefined()){
                def++;
            }
        }

        
        CachedSupplier<Map<Integer,Integer>> ringsizeMaker= CachedSupplier.of(()->ChemUtils.getSmallestRingSizeForEachBond(mol, 9));
        

        for(DoubleBondStereochemistry doubleBondStereochemistry : mol.getDoubleBondStereochemistry()) {
        	Bond doubleBond = doubleBondStereochemistry.getDoubleBond();


        	if (!doubleBondStereochemistry.getStereo().equals(DoubleBondStereochemistry.DoubleBondStereo.NONE)) {
        		boolean isRing=false;
        		try{
        			// This will only count a bond as a "ring bond" if the minimum ring size
        			// that that bond shows up in is less than 8 bonds long. Otherwise it's
        			// not considered a true ring bond and the double bond can be either E or Z.
        			//
        			// Note: this is still a simplification as there are other geometric effects which 
        			// can still make some double bonds ineligible for both E and Z.
        			if(doubleBond.isInRing()) {
	        			int smallestRing= ringsizeMaker.get().getOrDefault(mol.indexOf(doubleBond),999);
	        			if(smallestRing<8) {
	        				isRing=true;
	        			}
        			}
        		}catch(Exception e){
        			log.warn("Trouble detecting ring geometry for EZ calculation");
        		}

        		if(!isRing){                	 
        			ez++;
        		}
        	}
        }

        Chemical stdMol = mol.copy();
        if (standardize) {
            try {
                stdMol = standardizer.standardize(mol, molSupplier, struc.properties::add);
            } catch (Exception e) {
                log.error("Can't standardize structure", e);
            }
        }
//        if(!updatedMol){
//            standardizedMol = molSupplier.get();
//        }




        //Note that this currently uses the non-standardized structure instead of the standardized one.
        //This is currently intentional, as the standardized structure does some charge balancing that might be unexpected.
        //Nevertheless, there are times when a structure should really be prestandardized, and then have moieties generated

        List<Chemical> frags = mol.connectedComponentsAsStream().collect(Collectors.toList());

        // used to not duplicate moieties
        Map<String, Structure> moietiesMap = new HashMap<>();
        if (frags.size() >= 1 && components!=null) {
            for (Chemical frag : frags) {
                Structure moiety = new Structure();

                //TODO : This used to say not to standardize,
                // but it did anyways. Now it does whatever it would
                // have done at the root level.
                
                instrument(moiety, null, Chem.fixMetals(frag), settings.isStandardize());

                for(Value v:moiety.properties){
                    if(v instanceof Keyword){
                        if(((Keyword)v).label.equals(Structure.H_EXACT_HASH)){
                            String hash=((Keyword) v).term;
                            Structure s = moietiesMap.get(hash);
                            if(s!=null){
                                s.count++;
                            }else{
                                moietiesMap.put(hash,moiety);
                                if (components != null)
                                    components.add(moiety);
                            }
                            break;
                        }
                    }
                }
            }
        }


        try{
            Chemical cc=polymerSimplify(stdMol);
            // TODO: this only makes sense on standardization.
            // Need to evaluate that this call is intended as-is.
            
            hasher.hash(cc, cc.toMol(), new BiConsumer<String, String>() {
                @Override
                public void accept(String key, String value){
                    if(value==null || value.length() < 255) {
                        struc.properties.add(new Keyword(key, value));
                    }else{
                        log.debug("using Text!!! for " + value.length() + "  " + value);
                        struc.properties.add(new Text(key, value));
                    }
                }
            });

        }catch(Exception e){
            log.error("Error making structure hash", e);
        }


        struc.definedStereo = def;
        struc.stereoCenters = stereo;
        struc.ezCenters = ez;
        struc.charge = charge;
        //struc.formula = mol.getFormula();


        Chem.setFormula(struc);
        struc.setMwt(mol.getMass());

        if(!query){
            try {
                struc.smiles = standardizer.canonicalSmiles(struc, struc.molfile);
            }catch(Exception e) {}
        }
        
        calcStereo (struc);


    }


    private boolean isBondDiazo(Bond doubleBond) {
        log.trace("in isBondDiazo");
        if(doubleBond.getAtom1().getAtomicNumber()!=7 || doubleBond.getAtom2().getAtomicNumber()!=7) {
            return false;
        }
        //loop over all bonds connected to either atom of the double bond
        List bondSet = doubleBond.getAtom1().getBonds();
        bondSet.addAll(doubleBond.getAtom2().getBonds());
        
        for( Object b : bondSet) {
            Bond bond = (Bond)b;
            if( bond.equals(doubleBond) ) continue; //skip the starting bonds
            if( (bond.getAtom1() != doubleBond.getAtom1() && bond.getAtom1() != doubleBond.getAtom2() 
                    && bond.getBondType()==Bond.BondType.DOUBLE && bond.getAtom1().getAtomicNumber()==7) 
                    || (bond.getAtom2() != doubleBond.getAtom1() && bond.getAtom2() != doubleBond.getAtom2() 
                    && bond.getBondType()==Bond.BondType.DOUBLE && bond.getAtom2().getAtomicNumber()==7) ){
                log.trace("bond IS part of a diazo system");
                return true;
            }
        }
        return false;
    }
    public static Chemical polymerSimplify(Chemical chem){
        try{
            String nmol = Arrays.stream(chem.toMol().split("\n"))
                    .filter(ll->!ll.startsWith("M  S"))
                    .collect(Collectors.joining("\n"));

            Chemical cc= Chemical.parseMol(nmol);

            cc.atoms().filter(ca->ca.isQueryAtom())
                    .forEach(ca->{
                        //set all queries to helium as a hack
                        ca.setAtomicNumber(2);
                    });

            return cc;
        }catch(Exception e){
            return chem;
        }
    }
    
    

    /**
     * This should return a decomposed version of a structure for GSRS.
     *
     * This means that a molfile should come back with moieties
     * and a structure, with statistics and predicted stereo
     *
     * @param struc
     * @param components
     * @param mol
     * @param standardize
     */
    void instrument (Structure struc,
                            Collection<Structure> components,
                            Chemical mol,
                            boolean standardize) {
        StructureProcessorTask settings = new StructureProcessorTask.Builder()
                .structure(struc)
                .standardize(standardize)
                .query(false)
                .mol(mol)
                .components(components)
                .build();
        instrument(settings);
    }

    void calcStereo (Structure struc) {
        int total = struc.stereoCenters, defined = struc.definedStereo;
        if (total == 0) {
            struc.stereoChemistry = Structure.Stereo.ACHIRAL;
						//log.debug("in calcStereo prevening Optical.NONE");
            struc.opticalActivity = Structure.Optical.NONE;
        }
        else if (total == defined) {
            struc.stereoChemistry = Structure.Stereo.ABSOLUTE;
            struc.opticalActivity = Structure.Optical.UNSPECIFIED;
        }
        else if (total == 1 && defined == 0) {
            struc.stereoChemistry = Structure.Stereo.RACEMIC;
            struc.opticalActivity = Structure.Optical.PLUS_MINUS;
        }
        else if ((total - defined) == 1) {
            struc.stereoChemistry = Structure.Stereo.EPIMERIC;
            struc.opticalActivity = Structure.Optical.UNSPECIFIED;
        }
        else if ((total - defined) > 1) {
            struc.stereoChemistry = Structure.Stereo.MIXED;
            struc.opticalActivity = Structure.Optical.UNSPECIFIED;
        }
    }

    public static String toHex (byte[] binary) {
        StringBuilder sb = new StringBuilder ();

        for (int i = 0; i < binary.length; ++i) {
            sb.append(String.format("%1$02x", binary[i] & 0xff));
        }
        return sb.toString();
    }

    public static String digest (String s) {
        if (s != null) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA1");
                byte[] d = md.digest(s.getBytes("utf-8"));
                String digest = toHex (d);
                return digest;
            }
            catch (Exception ex) {
                log.error("Something's rotten in the state of MD", ex);
            }
        }
        return "deadbeef";
    }

    public Integer getComplexityCutoff() {
        return complexityCutoff;
    }

    public void setComplexityCutoff(Integer complexityCutoff) {
        this.complexityCutoff = complexityCutoff;
    }

    public Integer getMaxUndefined() {
        return maxUndefined;
    }

    public void setMaxUndefined(Integer maxUndefined) {
        this.maxUndefined = maxUndefined;
    }

}
