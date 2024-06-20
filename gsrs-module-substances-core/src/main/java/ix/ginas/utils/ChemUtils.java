package ix.ginas.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gov.nih.ncats.molwitch.Atom;
import gov.nih.ncats.molwitch.Bond;
import gov.nih.ncats.molwitch.Chemical;
import ix.core.models.Structure;
import ix.core.models.Structure.Optical;
import ix.core.models.Structure.Stereo;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.utils.FortranLikeParserHelper.LineParser;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChemUtils {

	private static LineParser MOLFILE_COUNT_LINE_PARSER = new LineParser("aaabbblllfffcccsssxxxrrrpppiiimmmvvvvvv");
	private static Pattern NEW_LINE_PATTERN = Pattern.compile("\n");

	/**
	 * Checks for basic valence problems on structure, adding warnings to the
	 * supplied list
	 *
	 * @param newstr the new Structure to check; can not be null.
	 * @param callback the validation callback to add the warning messages to.
	 */
	public static void checkValance(Structure newstr, ValidatorCallback callback) {
		List<GinasProcessingMessage> list = new ArrayList<>();
		//we can get away with delegating because there are no
		//apply changes invocations
		checkValance(newstr, list);
		for (GinasProcessingMessage m : list) {
			callback.addMessage(m);
		}
	}
	/**
	 * Checks for basic valence problems on structure, adding warnings to the
	 * supplied list
	 *
	 * @param newstr
	 * @param gpm
	 */
	public static void checkValance(Structure newstr, List<GinasProcessingMessage> gpm) {
		Chemical c = newstr.toChemical();
		for (Atom atom : c.getAtoms()) {
			if (atom.hasValenceError()) {
				GinasProcessingMessage mes = GinasProcessingMessage
								.WARNING_MESSAGE("Valence Error on " + atom.getSymbol() + " atom (" + (atom.getAtomIndexInParent() + 1) + ") ");
				gpm.add(mes);
			}
		}
	}

	/**
	 * Checks for basic charge balance of structure, warn if not 0
	 *
	 * @param newstr
	 * @param gpm
	 */
	public static void checkChargeBalance(Structure newstr, List<GinasProcessingMessage> gpm) {
		if (newstr.charge != 0) {
			GinasProcessingMessage mes = GinasProcessingMessage
							.WARNING_MESSAGE("Structure is not charge-balanced, net charge of: " + newstr.charge);
			gpm.add(mes);
		}
	}
	/**
	 * Checks for basic valence problems on structure, adding warnings to the
	 * supplied list
	 *
	 * @param newstr
	 * @param callback
	 */
	public static void fixChiralFlag(Structure newstr, ValidatorCallback callback) {
		List<GinasProcessingMessage> gpm = new ArrayList<>();
		fixChiralFlag(newstr, gpm);
		for (GinasProcessingMessage m : gpm) {
			callback.addMessage(m);
		}
	}
	/**
	 * Checks for basic chiral flag problems on structure, adding warnings to the
	 * supplied list, and fix the flag.
	 *
	 * @param newstr
	 * @param gpm
	 */
	public static void fixChiralFlag(Structure newstr, List<GinasProcessingMessage> gpm) {
		//Chiral flag should be on if:
		//1. There are defined centers
		//2. It's not explicitly racemic
		//3. It's not explicitly unknown
		//4. The optical activity is not (+/-)

		final String chiralFlagOn = "  1";
		final String chiralFlagOff = "  0";

		String newChiralFlag = chiralFlagOff;

		if (newstr.definedStereo > 0) {
			if (!Stereo.RACEMIC.equals(newstr.stereoChemistry) && !Stereo.UNKNOWN.equals(newstr.stereoChemistry)) {
				if (!Optical.PLUS_MINUS.equals(newstr.opticalActivity)) {
					newChiralFlag = chiralFlagOn;
				}
			}
		}

		String[] lines = NEW_LINE_PATTERN.split(newstr.molfile);
		if (lines.length < 3) {
			//guess it's a smiles? ignore
			return;
		}

		String chiralFlag = lines[3].substring(12, 15);

		if (!newChiralFlag.equals(chiralFlag)) {
			lines[3] = lines[3].substring(0, 12) + newChiralFlag + lines[3].substring(15);
			if (newChiralFlag.equals(chiralFlagOn)) {
				gpm.add(GinasProcessingMessage.INFO_MESSAGE("Adding chiral flag based on structure information"));
			}else{
				gpm.add(GinasProcessingMessage.INFO_MESSAGE("Removing chiral flag based on structure information"));
			}
		}
		lines[3] = MOLFILE_COUNT_LINE_PARSER.standardize(lines[3]);
		newstr.molfile = Arrays.stream(lines).collect(Collectors.joining("\n"));

	}

	public static void fix0Stereo(Structure newstr, List<GinasProcessingMessage> gpm) {
		String debugMessage = String.format("in fix0Stereo, newstr.stereoCenters: %d, stereochemistry: %s, optical: %s",
						newstr.stereoCenters, newstr.stereoChemistry.toString(), newstr.opticalActivity.toString());
		log.debug(debugMessage);
		if (newstr.stereoCenters == 0 && newstr.stereoChemistry.equals(Stereo.ACHIRAL)
						&& newstr.opticalActivity.equals(Optical.UNSPECIFIED)) {
			log.debug("detected 0 stereocenters, achiral and unspec optical condition");
			newstr.opticalActivity = Optical.NONE;
			gpm.add(GinasProcessingMessage.INFO_MESSAGE("Reset optical activity to NONE based on structure information"));
		}else	if( newstr.stereoChemistry.equals(Stereo.ABSOLUTE) && newstr.opticalActivity.equals(Optical.NONE)) {
			log.debug("detected absolute stereo and no optical activity condition");
			gpm.add(GinasProcessingMessage.WARNING_MESSAGE("Note: Stereo 'Absolute' with Optical Activity 'NONE' does not make sense!"));
		}
	}

	public static void checkRacemicStereo(Structure oldstr, ValidatorCallback callback) {
		String debugMessage = String.format("in checkRacemicStereo,  stereochemistry: %s, optical: %s, comments: %s",
						oldstr.stereoChemistry.toString(), oldstr.opticalActivity.toString(), oldstr.stereoComments);
		StringBuilder messageText = new StringBuilder();
		log.trace(debugMessage);
		if (oldstr.stereoChemistry.equals(Stereo.RACEMIC) && !oldstr.opticalActivity.equals(Optical.PLUS_MINUS)) {
			log.trace(" detected racemic/optical activity mismatch");
			if (oldstr.stereoComments == null || oldstr.stereoComments.length() == 0) {
				log.trace("no stereo comments");
				oldstr.opticalActivity = Optical.PLUS_MINUS;
				messageText.append("Note: when stereo is 'Racemic,' Optical Activity '+/-' is required unless there is a stereo comment! Optical activity will be reset when the substance is saved.");
			} else {
				messageText.append("Note: when stereo is 'Racemic,' Optical Activity '+/-' is strongly recommended.");
			}
			callback.addMessage(GinasProcessingMessage.WARNING_MESSAGE(messageText.toString()));
		}
	}

	public static void checkSgroups(Structure oldstr, ValidatorCallback callback) {
		List<String> sgroupWarnings = oldstr.toChemical().getImpl().getSGroupWarnings();
		sgroupWarnings.forEach(w-> callback.addMessage(GinasProcessingMessage.WARNING_MESSAGE(w)));
	}
	
	public static List<Bond> getNeighborBonds(Chemical c, Bond cb){
		List<Bond> bonds = new ArrayList<>();
		
		
		for(Atom at:streamAtoms(cb).collect(Collectors.toList())){
			at.getBonds()
			  .forEach(cbb->{
				  if(!cbb.equals(cb)){
					  bonds.add(cbb);
				  }
			  });
		}
		return bonds;
	}
	
	private static Stream<Atom> streamAtoms(Bond cb){
		return Stream.of(cb.getAtom1(),cb.getAtom2());
	}
	/**
	 * This is a ring-detection algorithm which colors edges based on their smallest detected ring, up to the maxRingSize
	 * specified. This is accomplished with a breadth-first search from each bond
	 * @param b
	 * @param m
	 * @return
	 */
	public static Map<Integer,Integer> getSmallestRingSizeForEachBond(Chemical b, int maxRingSize){

		//The computed return object
		Map<Integer,Integer> minRingForBond= new HashMap<>();
		
		// The neighbor bonds "adjacency" type map. Each bond points
		// to the set of bonds it shares an atom with
		Map<Integer,BitSet> nbonds= new HashMap<>();
		
		
		// Lookups to go from a bond or atom to its index
		// with some effort these could be eliminated
		Map<Bond, Integer> bondIndex = new HashMap<>();
		Map<Atom, Integer> atomIndex = new HashMap<>();
		
		//Cursor Bonds (left and right)
		Map<Integer,Set<Integer>> cursorBondsL= new HashMap<>();
		Map<Integer,Set<Integer>> cursorBondsR= new HashMap<>();
		
		//Cursor Atoms (left and right)
		Map<Integer,Set<Integer>> cursorAtomsL= new HashMap<>();
		Map<Integer,Set<Integer>> cursorAtomsR= new HashMap<>();
		Map<Integer,Set<Integer>> previousBondsL= new HashMap<>();
		Map<Integer,Set<Integer>> previousBondsR= new HashMap<>();
		Map<Integer,Set<Integer>> previousAtomsL= new HashMap<>();
		Map<Integer,Set<Integer>> previousAtomsR= new HashMap<>();
		
		
		for(int i=0;i<b.getAtomCount();i++){
			atomIndex.put(b.getAtom(i), i);
		}
		for(int i=0;i<b.getBondCount();i++){
			Bond cb=b.getBond(i);
			bondIndex.put(cb, i);
		}
		for(int i=0;i<b.getBondCount();i++){
			Bond cb=b.getBond(i);
//			if(cb.isRingBond()){
				previousBondsL.put(i,  Stream.of(i).collect(Collectors.toSet()));
				previousBondsR.put(i,  Stream.of(i).collect(Collectors.toSet()));
				
				previousAtomsL.put(i,  Stream.of(atomIndex.get(cb.getAtom1())).collect(Collectors.toSet()));
				previousAtomsR.put(i,  Stream.of(atomIndex.get(cb.getAtom2())).collect(Collectors.toSet()));
				
				cursorBondsL.put(i, cb.getAtom1().getBonds().stream().filter(bbb->!bbb.equals(cb)).map(cbb->bondIndex.get(cbb)).collect(Collectors.toSet()));
				cursorBondsR.put(i, cb.getAtom2().getBonds().stream().filter(bbb->!bbb.equals(cb)).map(cbb->bondIndex.get(cbb)).collect(Collectors.toSet()));
				
				Set<Integer> patsl= previousAtomsL.get(i);
				Set<Integer> patsr= previousAtomsR.get(i);
				
				cursorAtomsL.put(i, cursorBondsL.get(i).stream()
						                               .flatMap(bbi->streamAtoms(b.getBond(bbi)))
						                               .map(at->atomIndex.get(at))
						                               .filter(ati->!patsl.contains(ati))
						                               .collect(Collectors.toSet())
						                               );
				cursorAtomsR.put(i, cursorBondsR.get(i).stream()
                        .flatMap(bbi->streamAtoms(b.getBond(bbi)))
                        .map(at->atomIndex.get(at))
                        .filter(ati->!patsr.contains(ati))
                        .collect(Collectors.toSet())
                        );
//			}
		}
		for(int i=0;i<b.getBondCount();i++){
			Bond cb=b.getBond(i);
			BitSet bsBond=new BitSet(b.getBondCount());
			getNeighborBonds(b, cb)
				.stream()
				.map(cbb-> bondIndex.get(cbb))
				.filter(Objects::nonNull)
				.forEach(ii->{
					bsBond.set(ii);
				});
			nbonds.put(i,bsBond);
		}
		int[] cii= new int[]{0,0};
		int maxIter = (int) Math.ceil((maxRingSize-3)*0.5);
		for(int iter=0;iter<=maxIter;iter++){
			cii[1]=iter;
			for(int i=0;i<b.getBondCount();i++){
				cii[0]=i;

				Set<Integer> cbl= cursorBondsL.get(i);
				Set<Integer> cbr= cursorBondsR.get(i);
				Set<Integer> casl= cursorAtomsL.get(i);
				Set<Integer> casr= cursorAtomsR.get(i);
				
				if(minRingForBond.get(i)==null && cbl!=null && cbr!=null){
					boolean ringBonds= cbl.stream().filter(cali->cbr.contains(cali)).findAny().isPresent();
					if(ringBonds){
						minRingForBond.put(i, iter*2+2);
						continue;
					}
					if(iter*2+3<=maxRingSize){
						boolean ringatoms= casl.stream().filter(cali->casr.contains(cali)).findAny().isPresent();
						if(ringatoms){
							minRingForBond.put(i, iter*2+3); //iter*2+3 = maxRing  iter=(maxRing-3)/2
							continue;
						}
					}
					
					Set<Integer> pbr= previousBondsR.computeIfAbsent(i, k->{
						return new HashSet<>();
					});
					Set<Integer> pbl= previousBondsL.computeIfAbsent(i, k->{
						return new HashSet<>();
					});
					Set<Integer> par= previousAtomsR.computeIfAbsent(i, k->{
						return new HashSet<>();
					});
					Set<Integer> pal= previousAtomsL.computeIfAbsent(i, k->{
						return new HashSet<>();
					});
					Set<Integer> newCursorR = new HashSet<>();
					Set<Integer> newCursorL = new HashSet<>();
					Set<Integer> newAtomsR  = new HashSet<>();
					Set<Integer> newAtomsL  = new HashSet<>();
					
					cbl.stream()
					  .flatMap(j->nbonds.get(j).stream().mapToObj(ib->(Integer)ib))
					  .filter(bi->!pbl.contains(bi))
					  .filter(bi->!cbl.contains(bi))
					  .forEach(ii->{
						  newCursorL.add(ii);
					  });
					cbr.stream()
					  .flatMap(j->nbonds.get(j).stream().mapToObj(ib->(Integer)ib))
					  .filter(bi->!pbr.contains(bi))
					  .filter(bi->!cbl.contains(bi))
					  .forEach(ii->{
						  newCursorR.add(ii);
					  });
					
					newCursorL.stream()
					         .flatMap(ii->streamAtoms(b.getBond(ii)))
					         .map(ca->atomIndex.get(ca))
					         .filter(ii->!casl.contains(ii))
					         .forEach(ii->{
								  newAtomsL.add(ii);
							  });
					newCursorR.stream()
			         .flatMap(ii->streamAtoms(b.getBond(ii)))
			         .map(ca->atomIndex.get(ca))
			         .filter(ii->!casr.contains(ii))
			         .forEach(ii->{
						  newAtomsR.add(ii);
					  });
					         
					pbr.addAll(cbr);
					pbl.addAll(cbl);
					par.addAll(casr);
					pal.addAll(casl);
					
					cursorBondsR.put(i,newCursorR);
					cursorBondsL.put(i,newCursorL);
					cursorAtomsR.put(i,newAtomsR);
					cursorAtomsL.put(i,newAtomsL);
					
				}
			}
		}
		
		return minRingForBond;
	}
}
