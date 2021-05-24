package gsrs.module.substance.indexers;

import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.core.search.text.IndexableValueFromRaw;
import ix.ginas.models.v1.Substance;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;
@Component
public class NameCountIndexValueMaker implements IndexValueMaker<Substance>{
	@Override
	public Class<Substance> getIndexedEntityClass() {
		return Substance.class;
	}
	@Override
	public void createIndexableValues(Substance t, Consumer<IndexableValue> consumer) {
		int nc=t.getNameCount();
		consumer.accept(new IndexableValueFromRaw("Name Count", nc).dynamic());
	}

}
