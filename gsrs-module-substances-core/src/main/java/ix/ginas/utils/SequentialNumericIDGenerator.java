package ix.ginas.utils;

import gov.nih.ncats.common.sneak.Sneak;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class SequentialNumericIDGenerator<T> extends AbstractNoDependencyIDGenerator<T, String> implements NamedIdGenerator<T, String> {

	private int len;
	
	public String suffix;
	private boolean padding;
	
	public SequentialNumericIDGenerator(int len, String suffix, boolean padding){
		this.len=len;
		this.suffix=suffix;
		this.padding=padding;
	}
	
	public abstract long getNextNumber();
	
	public String adapt(long num){
		return num+"";
	}
	
	public synchronized String generateID() {
		long next=getNextNumber();
		String adapt=adapt(next);
		if((String.valueOf(next).length()+suffix.length())>this.len)  {
			log.warn("The nextNumber digit characters concatenated with suffix characters has length greater than 'len'");
			return Sneak.sneakyThrow(new Exception("The nextNumber digits combined with suffix has length greater than 'len'"));
		}
		if(padding){
			return padd(len-suffix.length()-adapt.length(), adapt);
		}
		return adapt + suffix;
	}

	private String padd(int len, String s){
		StringBuilder sb = new StringBuilder(len+ s.length() + suffix.length());
		for(int i=0;i<len;i++){
			sb.append("0");
		}
		sb.append(s).append(suffix);
		return sb.toString();
	}

	public int getLen()  {
		return this.len;
	}

	protected void setLen(int len)  {
		// this setter should probably not be called outside the instance's constructor.
		this.len=len;
	}

	public String getSuffix()  {
		return this.suffix;
	}

	protected void setSuffix(String suffix) {
		// this setter should probably not be called outside the instance's constructor.
		this.suffix=suffix;
	}


}