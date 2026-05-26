package ix.utils;


public class SubstanceLiteralReference<K>{
	private K o;
	public SubstanceLiteralReference(K o){
		this.o=o;
	}
	
	public K get(){
		return o;
	}
	@Override
	public int hashCode(){
		return this.o.hashCode();
	}
	@Override
	public boolean equals(Object oref){
		if(oref==null)return false;
		if(oref instanceof SubstanceLiteralReference){
			SubstanceLiteralReference<?> or=(SubstanceLiteralReference<?>)oref;
			return (this.o == or.o);
		}
		return false;
	}
	public static <K> SubstanceLiteralReference<K> of(K k) {
		return new SubstanceLiteralReference<K>(k);
	}
	
	public String toString(){
		return "Ref to:" + o.toString();
	}
}