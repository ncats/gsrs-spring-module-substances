package gsrs.module.substance.indexers;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;

import gsrs.repository.KeyUserListRepository;
import gsrs.repository.PrincipalRepository;
import ix.core.models.KeyUserList;
import ix.core.search.bulk.UserSavedListService;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.core.util.EntityUtils.EntityWrapper;
import ix.ginas.models.v1.Substance;
import ix.utils.Util;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserSavedListIndexValueMaker implements IndexValueMaker<Substance> {
	
	@Autowired
	public KeyUserListRepository keyUserListRepository;
	
	@Autowired
	public PrincipalRepository principalRepository;
	
		
	@Override
    public Class<Substance> getIndexedEntityClass() {
        return Substance.class;
    }
	
	@Override
	public Set<String> getFieldNames(){
		return Util.toSet("User List");
	}
		
	@Override
	public Set<String> getTags(){
		return Util.toSet("user_list");
	}
	
	@Override
    public void createIndexableValues(Substance substance, Consumer<IndexableValue> consumer) {
				
		try {
			String key = EntityWrapper.of(substance).getKey().toRootKey().getIdString();
	
			List<KeyUserList> list = keyUserListRepository.getAllListNamesFromKey(key, getIndexedEntityClass().getName());	
		
			list.forEach(listName -> {			
				String value = UserSavedListService.getIndexedValue(listName.principal.username, listName.listName);				
				consumer.accept(IndexableValue.simpleFacetStringValue("User List",value));
//				log.error("create indexed values: " + value);
			});
			
		}catch(NoSuchElementException nseExp) {
			if(substance.getName()!=null)
				log.warn("Cannot find the UUID of the substance: " + substance.getName());
			else
				log.warn("Cannot find the UUID and name of the substance.");
		}catch(Exception exp) {
			exp.printStackTrace();
		}	
	}
}
