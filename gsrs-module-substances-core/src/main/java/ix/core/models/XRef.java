package ix.core.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gsrs.model.GsrsApiAction;
import ix.core.ObjectResourceReference;
import ix.core.util.EntityUtils;
import ix.core.util.EntityUtils.EntityInfo;
import ix.core.util.EntityUtils.EntityWrapper;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name="ix_core_xref")
@Slf4j
@SequenceGenerator(name = "LONG_SEQ_ID", sequenceName = "ix_core_xref_seq", allocationSize = 1)
public class XRef extends IxModel {
    /**
     * not id of the XRef instance but id of the instance for which this
     * XRef is pointing to
     */
    @Column(nullable=false,length=40)
    public String refid; 
    @Column(length=255,nullable=false)
    public String kind;
    public boolean deprecated;
    
    @JsonIgnore
    @Transient
    @Indexable(indexed=false)
    public Object _instance; // instance of the object 

    @ManyToMany(cascade= CascadeType.ALL)
    @JoinTable(name="ix_core_xref_property")
    public List<Value> properties = new ArrayList<Value>();

    public XRef() {
    }

    public XRef(String kind, Long id) {
        this (kind, id.toString());
    }
    
    public XRef(String kind, UUID id) {
        this (kind, id.toString());
    }
    
    public XRef(String kind, String id) {
        if (id == null)
            throw new IllegalArgumentException
                ("Can't create XRef with no id");
        this.kind = kind;
        this.refid = id;
    }

    public XRef(Object instance) {
    	EntityWrapper ew = EntityWrapper.of(instance);
        if (!ew.isEntity())
            throw new IllegalArgumentException
                ("Can't create XRef for non-Entity instance");
        try {
            if (ew.hasKey()) {
                    this.refid = ew.getKey().getIdString();
            } else {
                    throw new IllegalArgumentException
                       (ew.getKind()+": Can't create XRef with null id!");
            }
            kind = ew.getKind();
        }catch (Exception ex) {
            throw new IllegalArgumentException (ex);
        }

        this._instance = instance;
    }
    //TODO katzelda Feb 2021: these deRef methods don't appear to be used in GSRS 2.x
    /*
    public Object deRef () {
        return deRef (false);
    }

    public Object deRef (boolean force) {
        if (_instance == null || force) {
            try {
            	EntityInfo ei = EntityUtils.getEntityInfoFor(kind);
            	if (!ei.hasIdField()){
                    throw new RuntimeException
                        ("Class "+kind+" doesn't have any fields "
                         +"annotated with @Id!");
                }
            	_instance=ei.findById(refid);
                
            }
            catch (Exception ex) {
                log.trace("Can't retrieve XRef "+kind+":"+refid, ex);
            }
        }
        return _instance;
    }
*/
    public Value addIfAbsent (Value value) {
        if (value != null) {
            if (value.id != null) {
                for (Value p : properties) {
                    if (value.id.equals(p.id))
                        return p;
                }
            }
            properties.add(value);
        }
        
        return value;
    }
    @JsonIgnore
    @GsrsApiAction("href")
    public ObjectResourceReference getHRef () {
//        return Global.getRef(kind, refid);
        return new ObjectResourceReference(kind, refid);
    }

    public boolean referenceOf (Object instance) {
        try {
            EntityInfo refEntityInfo = EntityUtils.getEntityInfoFor(kind);
            EntityWrapper ew = EntityWrapper.of(instance);
            if (ew.getEntityInfo().isParentOrChildOf(refEntityInfo)) {
                if (ew.getId().isPresent()) {
                    return refid.equals(ew.getKey().getIdString());
                }
                else {
                    log.error
                        ("Class "+ew.getKind()+" has no @Id annotation, or no Id found!");
                }
            }
        }
        catch (Exception ex) {
            log.trace("Can't retrieve class "+kind, ex);
        }
        return false;
    }
}
