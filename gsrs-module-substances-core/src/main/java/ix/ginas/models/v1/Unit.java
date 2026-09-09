package ix.ginas.models.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import tools.jackson.core.type.TypeReference;
import ix.core.SingleParent;
import ix.core.models.Indexable;
import ix.core.models.ParentReference;
import ix.core.models.VIntArray;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasCommonSubData;
import ix.ginas.models.serialization.IntArrayDeserializer;
import ix.ginas.models.serialization.IntArraySerializer;

import jakarta.persistence.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.json.JsonMapper;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Chemical Structual Fragment of a {@link Polymer}, typically
 * it is the repeated portion, aka a Structural Repeat Unit (SRU). {@link Unit}s
 * but may only be a fragment of an SRU that doesn't repeat.
 */
@Entity
@Table(name="ix_ginas_unit")
@SingleParent
public class Unit extends GinasCommonSubData {
	@ManyToOne(cascade = CascadeType.PERSIST)
	@ParentReference
	private Polymer owner;
	
	
    @OneToOne(cascade= CascadeType.ALL)
    @JsonSerialize(using = IntArraySerializer.class)
    @JsonDeserialize(using = IntArrayDeserializer.class)
    public VIntArray amap;

    @OneToOne(cascade= CascadeType.ALL)
    public Amount amount;
    public Integer attachmentCount;
    public String label;
    
    
    @Lob
    @Basic(fetch= FetchType.EAGER)
    @Indexable(indexed = false)
    public String structure;    //TODO: should be changed to be a structure
    
    public String type;
    
    @Lob
    @Column(name="attachmentMap")
    private String _attachmentMap;
    
    public Map<String,LinkedHashSet<String>> getAttachmentMap(){
		JsonMapper mapper = JsonMapper.builderWithJackson2Defaults().build();
    	Map<String, LinkedHashSet<String>> amap=null;
		try {
			amap = mapper.readValue(_attachmentMap, new TypeReference<Map<String, LinkedHashSet<String>>>(){});
		} catch (Exception e) {
			e.printStackTrace();
		}
    	return amap;
    }
    
    public void setAttachmentMap(Map<String,LinkedHashSet<String>> amap){
		JsonMapper mapper = JsonMapper.builderWithJackson2Defaults().build();
    	_attachmentMap=null;
    	try {
			_attachmentMap=mapper.writeValueAsString(amap);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    
    
    @JsonIgnore
    //TODO:Make this inspect the structure itself
    public List<String> getContainedConnections(){
    	//System.err.println("WARNING: SRU structure not validated to check for connection points");
    	List<String> contained=new ArrayList<String>();
    	
    	Pattern p = Pattern.compile("_(R[0-9][0-9]*)");
    	Matcher m = p.matcher(this.structure);

    	//System.out.println(this.structure);
    	while (m.find()) {
    		String rg=m.group(1);
    	    contained.add(rg);
    	   // System.out.println("Found contained:" + rg);
    	}
    	
    	return contained;
    }
    
    @JsonIgnore
    public List<String> getMentionedConnections(){
    	Map<String,LinkedHashSet<String>> mymap=this.getAttachmentMap();
    	List<String> conset=new ArrayList<>();
		if(mymap!=null){
			for(String k:mymap.keySet()){
				conset.add(k);
			}
		}
		return conset;
    }
    
    public void addConnection(String rgroup1, String rgroup2){
    	Map<String,LinkedHashSet<String>> amap=this.getAttachmentMap();
    	if(amap==null){
    		amap=new HashMap<>();
    	}
    	LinkedHashSet<String> set1=amap.get(rgroup1);
    	if(set1==null){
    		set1=new LinkedHashSet<String>();
    		amap.put(rgroup1, set1);
    	}
    	set1.add(rgroup2);
    	setAttachmentMap(amap);
    }


    @JsonIgnore
    public Polymer getPolymer(){
        return this.owner;
		}


    public Unit () {}


	  @Override
	   	@JsonIgnore
	   	public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences() {
	   		List<GinasAccessReferenceControlled> temp = new ArrayList<GinasAccessReferenceControlled>();

	   		if(this.amount!=null){
	   			temp.addAll(amount.getAllChildrenAndSelfCapableOfHavingReferences());
	   		}
	   		return temp;
	   	}
}
