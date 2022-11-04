package ix.core.processing;


import java.io.Closeable;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.Iterator;

public abstract class RecordExtractor<K> implements Closeable {
		
		public InputStream is;
		public RecordExtractor(InputStream is){
			this.is=is;
		}
		/**
		 * Gets the next record. Should return null if finished, and throw an exception if
		 * there's an error
		 * 
		 * @return
		 */
		abstract public K getNextRecord() throws Exception;
		abstract public void close();

	/**
	 * Get the Name for this Extractor.
	 * By basic, the name is the instance's class name.
	 * @return the name as a String can not be null.
	 *
	 */
		public String getExtractorName(){
			return getClass().getName();
		}
		
		public Iterator<K> getRecordIterator(){
			return new Iterator<K>(){
				private K cached=null;
				private Exception e=null;
				private boolean isCached=false;
				private boolean lastError=false;
				private boolean done=false;
				@Override
				public boolean hasNext() {
					if(done)return false;
					if(!isCached){
						cacheNext();
					}
					if(cached==null && !lastError){
						return false;
					}
					return true;
					
				}
				private void cacheNext(){
					this.e=null;
					lastError=false;
					
					try{
						cached = getNextRecord();
					}catch(Exception e){
						this.e=e;
						cached=null;
						lastError=true;
					}
					isCached=true;
				}

				@Override
				public K next() {
					if(!isCached)cacheNext();
					
					K ret=cached;
					Exception ex=this.e;
					if(ex!=null){
						throw new IllegalStateException(ex);
					}
					cacheNext();
					return ret;
					
				}

				@Override
				public void remove() {}
				
			};
		}		
		


		

		public static RecordExtractor getInstanceOfExtractor(String className){
			try{
				Class<?> clazz = Class.forName(className);
				Constructor<?> ctor = clazz.getConstructor(InputStream.class);
				RecordExtractor object = (RecordExtractor) ctor.newInstance(new Object[] { null });
				return object;
			}catch(Exception e){
				return null;
			}
		}

		
	}
