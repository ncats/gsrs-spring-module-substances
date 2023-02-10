package gsrs.module.substance.utils;

import ix.core.EntityFetcher;
import ix.core.search.bulk.ResultListRecord;
import ix.core.search.bulk.ResultListRecord.ResultListRecordBuilder;
import ix.core.search.bulk.ResultListRecordGenerator;
import ix.core.util.EntityUtils.Key;
import ix.ginas.models.v1.Substance;

public class SubstanceResultListRecordGenerator implements ResultListRecordGenerator{
	
	@Override
	public ResultListRecord generate(String keyString) {	
		ResultListRecordBuilder builder =  ResultListRecord.builder();
		if(keyString!=null) {
			Key key = Key.of(Substance.class, keyString);
			try {
				Substance s = (Substance)EntityFetcher.of(key).call();
				builder.key(key.toString());
				if(s.getDisplayName().isPresent()) {
					builder.displayName(s.getDisplayName().get().name);
				}
				builder.displayCode(s.approvalID);
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
		return builder.build();
	}
}
