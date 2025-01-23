package gsrs.module.substance.processors;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.services.GroupService;

import ix.core.EntityProcessor;
import ix.core.models.Group;
import ix.ginas.models.v1.Code;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Egor Puzanov
 */

public class SetAccessCodeProcessor implements EntityProcessor<Code>{

    private CachedSupplier initializer = CachedSupplier.runOnceInitializer(this::addGroupsIfNeeded);

    @Autowired
    private GroupService groupService;

    private Map<String, Map<String, Map<Integer, String>>> with;
    private Map<String, Set<Group>> codeSystemAccess;
    private Set<Group> defaultAccess;

    public SetAccessCodeProcessor(Map<String, Map<String, Map<Integer, String>>> with){
        this.with = with;
    }

    public void addGroupsIfNeeded(){
        Map<String, Set<Group>> csa = new HashMap<String, Set<Group>>();
        for (Map.Entry<String, Map<Integer, String>> e : with.getOrDefault("codeSystemAccess", new HashMap<String, Map<Integer, String>>()).entrySet()) {
            Set<Group> access = new LinkedHashSet<Group>();
            Map<Integer, String> group_list = e.getValue();
            if (group_list != null) {
                for (String groupName : group_list.values()){
                    access.add(groupService.registerIfAbsent(groupName));
                }
            }
            csa.put(e.getKey(), access);
        }
        if (!csa.containsKey("*")) {
            csa.put("*", new LinkedHashSet<Group>());
        }
        this.defaultAccess = csa.remove("*");
        this.codeSystemAccess = csa;
    }

    @Override
    public void initialize() throws EntityProcessor.FailProcessingException{
        initializer.getSync();
    }

    @Override
    public void prePersist(Code obj) throws EntityProcessor.FailProcessingException {
        Set<Group> access = codeSystemAccess.getOrDefault(obj.codeSystem, defaultAccess);
        if(!access.equals(obj.getAccess())){
            obj.setAccess(access);
        }
    }

    @Override
    public Class<Code> getEntityClass() {
        return Code.class;
    }
}
