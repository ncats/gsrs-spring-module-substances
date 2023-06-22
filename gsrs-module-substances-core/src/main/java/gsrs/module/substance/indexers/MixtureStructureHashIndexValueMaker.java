package gsrs.module.substance.indexers;

import gsrs.module.substance.repository.SubstanceRepository;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.MixtureSubstance;
import ix.ginas.models.v1.Substance;
import ix.utils.UUIDUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;


/**
 * Adds structure hash index values to index for the chemical components of a mixture
 * 
 * @author peryeata
 *
 */
@Component
public class MixtureStructureHashIndexValueMaker implements IndexValueMaker<Substance>{
	@Autowired
	private SubstanceRepository substanceRepository;

	@Override
	public Class<Substance> getIndexedEntityClass() {
		return Substance.class;
	}
	//This is the method which does the work
	@Override
	public void createIndexableValues(Substance s, Consumer<IndexableValue> consumer) {
		
		if(s instanceof MixtureSubstance){
			createMixtureLychis((MixtureSubstance)s, consumer);
		}
	}
	
	
	public void createMixtureLychis(MixtureSubstance s, Consumer<IndexableValue> consumer) {
		s.mixture.components
		         .stream()
		         .forEach(mc->{
		        	 String refuuid=mc.substance.refuuid;
		        	 if(UUIDUtil.isUUID(refuuid)){
		        		 Substance component = substanceRepository.findBySubstanceReference(mc.substance);
		        		 if(component instanceof ChemicalSubstance){
		        			 extractStructureHashes((ChemicalSubstance)component, consumer);
		        		 }
		        	 }
		         });
		         
	}
	
	public void extractStructureHashes(ChemicalSubstance s, Consumer<IndexableValue> consumer) {
		
		//consumer.accept(IndexableValue.simpleStringValue("root_structure_properties_term", lychi3));
	    

        String stereoInsensitive=s.getStructure().getStereoInsensitiveHash();
        String exact=s.getStructure().getExactHash();          
        
        ChemicalSubstanceStructureHashIndexValueMaker.addHashes(stereoInsensitive, exact, "root_structure_properties", consumer);

        
		s.moieties.stream().forEach(m->{
		    String sins=m.structure.getStereoInsensitiveHash();
            String exa=m.structure.getExactHash();
            ChemicalSubstanceStructureHashIndexValueMaker.addHashes(sins, exa, "root_moieties_properties", consumer);
		});
		
	}
	
}
