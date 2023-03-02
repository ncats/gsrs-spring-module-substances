package gsrs.module.substance.indexers;

import java.util.List;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;

import gsrs.repository.KeyUserListRepository;
import gsrs.repository.PrincipalRepository;
import gsrs.security.GsrsSecurityUtils;
import ix.core.models.Principal;
import ix.core.search.bulk.UserSaveListService;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.core.util.EntityUtils.EntityWrapper;
import ix.ginas.models.v1.Substance;
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
    public void createIndexableValues(Substance substance, Consumer<IndexableValue> consumer) {
		
				
    	if(!GsrsSecurityUtils.getCurrentUsername().isPresent()) return;  	    	
    	String userName = GsrsSecurityUtils.getCurrentUsername().get();
    	log.warn("UserSavedListIndexValueMaker " + " username: " + userName);
    	Principal user = principalRepository.findDistinctByUsernameIgnoreCase(userName);
		if(user == null)
			return;
		String key = EntityWrapper.of(substance).getKey().toRootKey().getIdString();
	
		List<String> list = keyUserListRepository.getAllListNamesFromKey(key, user.id);
		list.forEach(e->log.warn("list name " + e));
		
		list.forEach(listName -> {			
			String value = UserSaveListService.getIndexedValue(userName, listName);
			log.warn("value: " + value);
			consumer.accept(IndexableValue.simpleFacetStringValue("User List",value));});
	
	}
}
