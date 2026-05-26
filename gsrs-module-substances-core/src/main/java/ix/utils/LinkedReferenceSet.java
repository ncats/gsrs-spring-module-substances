package ix.utils;

import ix.core.util.UniqueStack;

import java.util.Optional;
import java.util.stream.Stream;

public class LinkedReferenceSet<K> implements ExecutionStack<K> {
	UniqueStack<SubstanceLiteralReference<K>> internalStack = new UniqueStack<SubstanceLiteralReference<K>>();
	
	
	public boolean contains(K k){
		return internalStack.contains(SubstanceLiteralReference.of(k));
	}
	
	
	@Override
	public void pushAndPopWith(K obj, Runnable r) {
		internalStack.pushAndPopWith(SubstanceLiteralReference.of(obj), r);
	}

	@Override
	public K getFirst() {
		return internalStack.getFirst().get();
	}
	
	@Override
	public Optional<K> getOptionalFirst() {
		Optional<SubstanceLiteralReference<K>> ret =internalStack.getOptionalFirst();
		if(ret.isPresent()){
			return Optional.of(ret.get().get());
		}
		return Optional.empty();
	}

	@Override
	public void setMaxDepth(Integer maxDepth) {
		internalStack.setMaxDepth(maxDepth);
	}

	public Stream<K> asStream(){
		return internalStack.asStream().map(l->l.get());
	}
	
	
}