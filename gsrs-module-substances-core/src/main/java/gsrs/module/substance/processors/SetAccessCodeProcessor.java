package gsrs.module.substance.processors;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.services.GroupService;

import ix.core.EntityProcessor;
import ix.core.models.Group;
import ix.ginas.models.v1.Code;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Egor Puzanov
 */

public class SetAccessCodeProcessor implements EntityProcessor<Code>{

    private CachedSupplier<Void> initializer = CachedSupplier.runOnceInitializer(this::addGroupsIfNeeded);

    @Autowired
    private GroupService groupService;

    private final Map<String, Map<Object, Object>> with;
    private final Map<String, Set<Group>> codeSystemAccess = new HashMap<String, Set<Group>>();
    private final Set<Group> defaultAccess = new LinkedHashSet<Group>();

    public SetAccessCodeProcessor(Map<String, Map<Object, Object>> with){
        this.with = with;
    }

    public SetAccessCodeProcessor(){
        this(new HashMap<String, Map<Object, Object>>());
    }

    public void setCodeSystemAccess(Map<Object, Object> o) {
        this.with.put("codeSystemAccess", o);
    }

    public Map<String, Set<Group>> getCodeSystemAccess() {
        return this.codeSystemAccess;
    }

    public void setDefaultAccess(String o) {
        setDefaultAccess(new HashMap<Object, Object>());
    }

    public void setDefaultAccess(Map<Object, Object> o) {
        this.with.put("defaultAccess", o);
    }

    public Set<Group> getDefaultAccess() {
        return this.defaultAccess;
    }

    public void addGroupsIfNeeded(){
        defaultAccess.addAll(parseAccess(with.get("defaultAccess")));

        for (Map.Entry<Object, Object> e : with.getOrDefault("codeSystemAccess", new HashMap<Object, Object>()).entrySet()) {
            codeSystemAccess.put(String.valueOf(e.getKey()), parseAccess(e.getValue()));
        }
    }

    private Set<Group> parseAccess(Object accessObj) {
        Set<Group> access = new LinkedHashSet<Group>();
        if (accessObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> accessMap = (Map<Object, Object>) accessObj;
            for (Object groupName : accessMap.values()) {
                access.add(groupService.registerIfAbsent(String.valueOf(groupName)));
            }
        }
        return access;
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
