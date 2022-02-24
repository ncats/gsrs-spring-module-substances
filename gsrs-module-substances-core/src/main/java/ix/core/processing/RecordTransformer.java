package ix.core.processing;

import ix.core.models.ProcessingRecord;


public abstract class RecordTransformer<K,T>{
		public abstract T transform(PayloadExtractedRecord<K> pr, ProcessingRecord rec) throws Exception;
	}
	
