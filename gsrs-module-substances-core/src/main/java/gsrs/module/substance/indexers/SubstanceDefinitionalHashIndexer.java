/*
 * Get the Definitional Elements collection for the Substance -- which comes from a class-specific routine
 * and create a hash that will enable (Lucene) searching.
 */
package gsrs.module.substance.indexers;

import gsrs.module.substance.definitional.DefinitionalElements;
import gsrs.module.substance.services.DefinitionalElementFactory;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.core.util.LogUtil;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 *
 * @author Mitch Miller
 */
@Slf4j
@Component
public class SubstanceDefinitionalHashIndexer implements IndexValueMaker<Substance>
{

	@Autowired
	private DefinitionalElementFactory definitionalElementFactory;

	@Override
	public Class<Substance> getIndexedEntityClass() {
		return Substance.class;
	}
	@Override
	public void createIndexableValues(Substance substance, Consumer<IndexableValue> consumer)
	{
		LogUtil.trace(()->String.format("Starting in SubstanceDefinitionalHashIndexer.createIndexableValues. class: %s ",
						substance.getClass().getName()));
		try
		{
			DefinitionalElements elements =  definitionalElementFactory.computeDefinitionalElementsFor(substance);

			LogUtil.trace(()->String.format(" received %d elements", elements.getElements().size()));
			if( elements==null)
			{
				log.trace("elements null");
				return;
			}
			if( elements.getElements().isEmpty()) 
			{
				log.trace("elements empty");
				return;
			}
			
			List<String> layerHashes = elements.getDefinitionalHashLayers();
			log.trace(String.format(" %d layers", layerHashes.size()));
			for (int layer = 1; layer <= layerHashes.size(); layer++)
			{
				String layerName = "root_definitional_hash_layer_" + layer;
				log.trace("layerName: " + layerName + ":" + layerHashes.get(layer - 1));
				consumer.accept(IndexableValue.simpleStringValue(layerName, layerHashes.get(layer - 1)));
			}
		} catch (Exception ex)
		{
			log.error("Error during indexing", ex);
			ex.printStackTrace();
		}
	}

}
