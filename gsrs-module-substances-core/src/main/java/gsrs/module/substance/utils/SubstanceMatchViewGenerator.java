package gsrs.module.substance.utils;

import org.springframework.stereotype.Service;

import ix.core.EntityFetcher;
import ix.core.search.bulk.BulkSearchResult;
import ix.core.search.bulk.MatchView;
import ix.core.search.bulk.MatchViewGenerator;
import ix.core.util.EntityUtils.Key;
import ix.ginas.models.v1.Substance;

@Service
public class SubstanceMatchViewGenerator implements MatchViewGenerator{
	
	private String displayCodeName;
	
	public SubstanceMatchViewGenerator(){
		displayCodeName = "UNII";
	}
	
	public SubstanceMatchViewGenerator(String dcn){
		displayCodeName = dcn;
	}
	
	@Override
	public MatchView generate(BulkSearchResult bsr) {		
		MatchView.MatchViewBuilder builder = MatchView.builder();
		if(bsr != null && bsr.getKey() != null) {
			Key key = bsr.getKey();
			builder.id(key.getIdString());
			try {
				Substance s = (Substance)EntityFetcher.of(key).call();
				if(s.getDisplayName().isPresent()) {
					builder.displayName(s.getDisplayName().get().name);
				}
				//todo: evaluate whether to use substance summary 
				builder.displayCodeName(displayCodeName);
				builder.displayCode(s.approvalID);
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
		return builder.build();
	}
}
