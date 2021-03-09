package gsrs.module.substance.kew;

import java.util.Iterator;

public interface DataSet<K> extends Iterable<K>{
	public Iterator<K> iterator();
	public boolean contains(K k);
}
