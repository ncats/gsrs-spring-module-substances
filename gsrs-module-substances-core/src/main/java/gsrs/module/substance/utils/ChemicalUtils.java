package gsrs.module.substance.utils;

import gov.nih.ncats.molwitch.Atom;
import gov.nih.ncats.molwitch.Bond;
import gov.nih.ncats.molwitch.Chemical;
import gsrs.module.substance.StructureHandlingConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ChemicalUtils {
    private final static String electroneglist=
            "H	hydrogen	2.2	non-metal\n" +
                    "He	helium	no data 	non-metal\n" +
                    "Li	lithium	0.98	metal\n" +
                    "Be	beryllium	1.57	metal\n" +
                    "B	boron	2.04	\n" +
                    "C	carbon	2.55	non-metal\n" +
                    "N	nitrogen	3.04	non-metal\n" +
                    "O	oxygen	3.44	non-metal\n" +
                    "F	fluorine	3.98	non-metal\n" +
                    "Ne	neon	no data 	non-metal\n" +
                    "Na	sodium	0.93	metal\n" +
                    "Mg	magnesium	1.31	metal\n" +
                    "Al	aluminium	1.61	metal\n" +
                    "Si	silicon	1.9	\n" +
                    "P	phosphorus	2.19	non-metal\n" +
                    "S	sulfur	2.58	non-metal\n" +
                    "Cl	chlorine	3.16	non-metal\n" +
                    "Ar	argon	no data 	non-metal\n" +
                    "K	potassium	0.82	metal\n" +
                    "Ca	calcium	1	metal\n" +
                    "Sc	scandium	1.36	metal\n" +
                    "Ti	titanium	1.54	metal\n" +
                    "V	vanadium	1.63	metal\n" +
                    "Cr	chromium	1.66	metal\n" +
                    "Mn	manganese	1.55	metal\n" +
                    "Fe	iron	1.83	metal\n" +
                    "Co	cobalt	1.88	metal\n" +
                    "Ni	nickel	1.91	metal\n" +
                    "Cu	copper	1.9	metal\n" +
                    "Zn	zinc	1.65	metal\n" +
                    "Ga	gallium	1.81	metal\n" +
                    "Ge	germanium	2.01	\n" +
                    "As	arsenic	2.18	\n" +
                    "Se	selenium	2.55	non-metal\n" +
                    "Br	bromine	2.96	non-metal\n" +
                    "Kr	krypton	3	non-metal\n" +
                    "Rb	rubidium	0.82	metal\n" +
                    "Sr	strontium	0.95	metal\n" +
                    "Y	yttrium	1.22	metal\n" +
                    "Zr	zirconium	1.33	metal\n" +
                    "Nb	niobium	1.6	metal\n" +
                    "Mo	molybdenum	2.16	metal\n" +
                    "Tc	technetium	1.9	metal\n" +
                    "Ru	ruthenium	2.2	metal\n" +
                    "Rh	rhodium	2.28	metal\n" +
                    "Pd	palladium	2.2	metal\n" +
                    "Ag	silver	1.93	metal\n" +
                    "Cd	cadmium	1.69	metal\n" +
                    "In	indium	1.78	metal\n" +
                    "Sn	tin	1.96	metal\n" +
                    "Sb	antimony	2.05	\n" +
                    "Te	tellurium	2.1	\n" +
                    "I	iodine	2.66	non-metal\n" +
                    "Xe	xenon	2.6	non-metal\n" +
                    "Cs	caesium	0.79	metal\n" +
                    "Ba	barium	0.89	metal\n" +
                    "La	lanthanum	1.1	metal\n" +
                    "Ce	cerium	1.12	metal\n" +
                    "Pr	praseodymium	1.13	metal\n" +
                    "Nd	neodymium	1.14	metal\n" +
                    "Pm	promethium	no data 	metal\n" +
                    "Sm	samarium	1.17	metal\n" +
                    "Eu	europium	no data 	metal\n" +
                    "Gd	gadolinium	1.2	metal\n" +
                    "Tb	terbium	no data 	metal\n" +
                    "Dy	dysprosium	1.22	metal\n" +
                    "Ho	holmium	1.23	metal\n" +
                    "Er	erbium	1.24	metal\n" +
                    "Tm	thulium	1.25	metal\n" +
                    "Yb	ytterbium	no data 	metal\n" +
                    "Lu	lutetium	1.27	metal\n" +
                    "Hf	hafnium	1.3	metal\n" +
                    "Ta	tantalum	1.5	metal\n" +
                    "W	tungsten	2.36	metal\n" +
                    "Re	rhenium	1.9	metal\n" +
                    "Os	osmium	2.2	metal\n" +
                    "Ir	iridium	2.2	metal\n" +
                    "Pt	platinum	2.28	metal\n" +
                    "Au	gold	2.54	metal\n" +
                    "Hg	mercury	2	metal\n" +
                    "Tl	thallium	1.62	metal\n" +
                    "Pb	lead	2.33	metal\n" +
                    "Bi	bismuth	2.02	metal\n" +
                    "Po	polonium	2	metal\n" +
                    "At	astatine	2.2	\n" +
                    "Rn	radon	no data 	non-metal\n" +
                    "Fr	francium	0.7	metal\n" +
                    "Ra	radium	0.9	metal\n" +
                    "Ac	actinium	1.1	metal\n" +
                    "Th	thorium	1.3	metal\n" +
                    "Pa	protactinium	1.5	metal\n" +
                    "U	uranium	1.38	metal\n" +
                    "Np	neptunium	1.36	metal\n" +
                    "Pu	plutonium	1.28	metal\n" +
                    "Am	americium	1.3	metal\n" +
                    "Cm	curium	1.3	metal\n" +
                    "Bk	berkelium	1.3	metal\n" +
                    "Cf	californium	1.3	metal\n" +
                    "Es	einsteinium	1.3	metal\n" +
                    "Fm	fermium	1.3	metal\n" +
                    "Md	mendelevium	1.3	metal\n" +
                    "No	nobelium	1.3	metal";

    @Autowired
    private StructureHandlingConfiguration structureHandlingConfiguration;

    private Map<String, String> saltData;

    @PostConstruct
    private void setUpSalts() {
        log.warn("in setUpSalts, structureHandlingConfiguration.getSaltFilePath(): {}", structureHandlingConfiguration.getSaltFilePath());
        saltData = new HashMap<>();
        try {
            File file = new File(structureHandlingConfiguration.getSaltFilePath());
            Assert.assertTrue("input salt data file must exist!", file.exists());
            List<String> lines = Files.readAllLines(file.toPath());
            for (String line : lines) {
                String[] lineParts = line.split("\\t");
                saltData.put(lineParts[0], lineParts[1]);
            }
        }
        catch (Exception ex){
            log.error("Error loading salt data {}", ex.getMessage());
            ex.printStackTrace();
        }
        log.warn("completed loading of salt data -- {} records", saltData.keySet().size());
    }

    public static class MetalicNature{
        enum ATOM_METAL_TYPE{
            METAL,
            NONMETAL,
            METALLIOD
        }
        double en;
        ATOM_METAL_TYPE atype;
        public MetalicNature(double en, ATOM_METAL_TYPE atype){
            this.en=en;
            this.atype=atype;
        }
        public boolean shouldBeIonic(MetalicNature mn2){
            //if(Math.abs(this.en-mn2.en)>1.6){
            //}
            return (this.atype == ATOM_METAL_TYPE.METAL && mn2.atype == ATOM_METAL_TYPE.NONMETAL) ||
                    (this.atype == ATOM_METAL_TYPE.NONMETAL && mn2.atype == ATOM_METAL_TYPE.METAL);
        }
        public boolean isMetal(){
            return ATOM_METAL_TYPE.METAL==this.atype;
        }
        public boolean isNonMetal(){
            return ATOM_METAL_TYPE.NONMETAL==this.atype;
        }

    }
    static Map<String,MetalicNature> EN_MAP = new ConcurrentHashMap<String,MetalicNature>();
    static{
        for(String line:electroneglist.split("\n")){
            String[] parts=line.split("\t");
            double score = -1;
            try{
                score = Double.valueOf(parts[2]);
            }catch(Exception e){

            }

            String symbol=parts[0];
            MetalicNature.ATOM_METAL_TYPE atype= MetalicNature.ATOM_METAL_TYPE.METALLIOD;
            try{
                atype= MetalicNature.ATOM_METAL_TYPE.valueOf(parts[3].toUpperCase().replace("-", ""));
            }catch(Exception e){

            }

            EN_MAP.put(symbol,new MetalicNature(score, atype));

        }
    }


    public static boolean breakBondsToMetals(Chemical c){
        Set<Bond> toRemove = new HashSet<Bond>();

        for (Atom ca : c.getAtoms()) {
            MetalicNature en1 = EN_MAP.get(ca.getSymbol());
            if(en1==null)continue;
            for (Atom ca2 : ca.getNeighbors()) {

                for (Bond cb : ca.getBonds()) {
                    if (cb.getOtherAtom(ca).equals(ca2)) {
                        MetalicNature en2=EN_MAP.get(ca2.getSymbol());
                        if(en2!=null){
                            if(en1.shouldBeIonic(en2)){

                                if(toRemove.add(cb)){
                                    int order=cb.getBondType().getOrder();
                                    if(en1.isMetal()){
                                        ca.setCharge(ca.getCharge()+order);
                                        ca2.setCharge(ca2.getCharge()-order);
                                    }else{
                                        ca.setCharge(ca.getCharge()-order);
                                        ca2.setCharge(ca2.getCharge()+order);
                                    }
                                }
                            }
                        }
                    }
                    //}
                }
            }
        }
        boolean changed=false;
        for(Bond cb1:toRemove){
            c.removeBond(cb1);
            changed=true;
        }
        return changed;
    }

    public static boolean removeMetals(Chemical c){
        Set<Bond> toRemove = new HashSet<Bond>();
        Set<Atom> atomsToRemove = new HashSet<>();
        for (Atom ca : c.getAtoms()) {
            MetalicNature en1 = EN_MAP.get(ca.getSymbol());
            if(en1==null)continue;
            for (Atom ca2 : ca.getNeighbors()) {
                for (Bond cb : ca.getBonds()) {
                    if (cb.getOtherAtom(ca).equals(ca2)) {
                        MetalicNature en2=EN_MAP.get(ca2.getSymbol());
                        if(en2!=null){
                            if(en1.shouldBeIonic(en2)){

                                if(toRemove.add(cb)){
                                    int order=cb.getBondType().getOrder();
                                    if(en1.isMetal()){
                                        //ca.setCharge(ca.getCharge()+order);
                                        ca2.setCharge(ca2.getCharge()-order);
                                        atomsToRemove.add(ca);
                                    }else{
                                        ca.setCharge(ca.getCharge()-order);
                                        //ca2.setCharge(ca2.getCharge()+order);
                                        atomsToRemove.add(ca2);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        boolean changed=false;
        for(Bond cb1:toRemove){
            c.removeBond(cb1);
            changed=true;
        }
        for(Atom a:atomsToRemove){
            c.removeAtom(a);
            changed=true;
        }
        return changed;
    }

    public Chemical stripSalts(Chemical inputChemical) {
        List<Chemical> chemicals = new ArrayList<>();
        inputChemical.getConnectedComponents().forEach(c ->
        {
            String componentInchiKey = "";
            try  {
                componentInchiKey =c.toInchi().getKey();
            }
            catch(IOException ex) {
                log.error("Error computing inchi for fragment ", ex);
            }
            if (!saltData.containsKey(componentInchiKey))
            {
                chemicals.add(c);
            }
        });
        if (chemicals.size() == 1)
        {
            return chemicals.get(0);
        }
        if (!chemicals.isEmpty())
        {
            int maxAtoms = 0;
            int indexOfMaxAtoms = -1;
            for (int i = 0; i < chemicals.size(); i++)
            {
                Chemical chemical = chemicals.get(i);
                if (chemical.getAtomCount() > maxAtoms)
                {
                    maxAtoms = chemical.getAtomCount();
                    indexOfMaxAtoms = i;
                }
            }
            return chemicals.get(indexOfMaxAtoms);
        }
        return null;
    }

}
