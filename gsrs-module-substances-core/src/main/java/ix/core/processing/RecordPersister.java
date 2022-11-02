package ix.core.processing;




import gsrs.springUtils.AutowireHelper;

import java.lang.reflect.Constructor;

public abstract class RecordPersister<K,T>{

	/**
	 * Get the Name for this Persister.
	 * By basic, the name is the instance's class name.
	 * @return the name as a String can not be null.
	 *
	 */
	public String getPersisterName(){
		return getClass().getName();
	}

		public abstract void persist(TransformedRecord<K,T> prec) throws Exception;

		public static RecordPersister getInstanceOfPersister(String className) {
			try{
				Class<?> clazz = Class.forName(className);
				Constructor<?> ctor = clazz.getConstructor();
				RecordPersister object = (RecordPersister) ctor.newInstance();
				//needed for @Transactional to work
				return AutowireHelper.getInstance().autowireAndProxy(object);
			}catch(Exception e){
				return null;
			}
		}		
	}
	
