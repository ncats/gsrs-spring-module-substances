package ix.ginas.models.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import gsrs.module.substance.utils.HtmlUtil;
import gsrs.springUtils.StaticContextAccessor;
import ix.core.SingleParent;
import ix.core.models.*;
import ix.ginas.models.CommonDataElementOfCollection;
import ix.ginas.models.EmbeddedKeywordList;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasCommonData;
import ix.ginas.models.serialization.KeywordDeserializer;
import ix.ginas.models.serialization.KeywordListSerializer;
import ix.ginas.models.utils.JSONConstants;
import ix.ginas.models.utils.JSONEntity;
import org.apache.commons.lang3.ObjectUtils;

import javax.persistence.*;
import java.util.*;

@JSONEntity(title = "Name", isFinal = true)
@Entity
@Table(name="ix_ginas_name", indexes = {@Index(name = "name_index", columnList = "name"),
                                        @Index(name = "name_owner_index", columnList = "owner_uuid")})
@SingleParent
@IndexableRoot
public class Name extends CommonDataElementOfCollection {
	
	
	public static enum Sorter implements  Comparator<Name> {

		/**
		 * Utility function to sort names in nice display order.
		 * <p>
		 * Sort criteria: </p>
		 * <ol>
		 * <li> Display Name </li>
		 * <li> Preferred status</li>
		 * <li> Official status</li>
		 * <li> English first</li>
		 * <li> Alphabetical</li>
		 * <li> Name Type</li>
		 * <li> Number of References</li>
		 *
		 *
		 * </ol>
		 *
		 * Note that this sort order was changed in September 2018
		 * for v2.3.1 so sorting with older versions might
		 * be slightly different.
		 */
		DISPlAY_NAME_FIRST_ENGLISH_FIRST{
			public int compare(Name o1, Name o2) {
				if(o1.isDisplayName()!= o2.isDisplayName()){
					if(o1.isDisplayName())return 1;
					return -1;
				}
				if(o1.preferred!=o2.preferred){
					if(o2.preferred)return 1;
					return -1;
				}
				if(o1.isOfficial()!=o2.isOfficial()){
					if(o2.isOfficial())return 1;
					return -1;
				}
				if(o1.isLanguage("en")!=o2.isLanguage("en")){
					if(o2.isLanguage("en"))return 1;
					return -1;
				}
				//katzelda GSRS-623 : changed sort order
				//from #refs, type, alpha -> alpha, type, #refs
				int nameCompare = ObjectUtils.compare(o1.name, o2.name);
				if(nameCompare !=0){
					return nameCompare;
				}

				int nameType = ObjectUtils.compare(o1.type, o2.type);
				if(nameType !=0){
					return nameType;
				}
				return o2.getReferences().size()-o1.getReferences().size();

			}
		},
		BY_CREATION_DATE{
            @Override
            public int compare(Name o1, Name o2) {
                return o1.getCreated().compareTo(o2.getCreated());
            }
        }
	}



    private static final String SRS_LOCATOR = "SRS_LOCATOR";

	@ManyToOne(cascade = CascadeType.PERSIST)
	@JsonIgnore
	@ParentReference
	private Substance owner;

	public Substance fetchOwner(){
		return this.owner;
	}
	public Substance getOwner(){
		return this.owner;
	}

	public void setOwner(Substance owner) {
		this.owner = owner;
	}

	public void assignOwner(Substance own){
		this.owner=own;
	}

    @JSONEntity(title = "Name", isRequired = true)
    @Column(nullable=false, length=1024)
    @Indexable(name="Name", suggest=true)
    public String name;

    @Lob
    @Basic(fetch= FetchType.EAGER)
    @JsonIgnore
    public String fullName;
    
    @Lob
    @Basic(fetch= FetchType.EAGER)
    //@JsonView(BeanViews.JsonDiff.class)  commenting this out to make the stdName field easier to see
	@Indexable(name="Standardized Name", suggest=true)
	public String stdName;
    
    
    @JSONEntity(title = "Name Type", format = JSONConstants.CV_NAME_TYPE, values = "JSONConstants.ENUM_NAMETYPE")
    @Column(length=32)
    public String type="cn";
    
    @JSONEntity(title = "Domains", format = "table", itemsTitle = "Domain", itemsFormat = JSONConstants.CV_NAME_DOMAIN)
    @JsonSerialize(using= KeywordListSerializer.class)
    @JsonDeserialize(contentUsing= KeywordDeserializer.DomainDeserializer.class)
    @Basic(fetch= FetchType.LAZY)
    public EmbeddedKeywordList domains = new EmbeddedKeywordList();
    
    @JSONEntity(title = "Languages", format = "table", itemsTitle = "Language", itemsFormat = JSONConstants.CV_LANGUAGE)
    @JsonSerialize(using= KeywordListSerializer.class)
    @JsonDeserialize(contentUsing= KeywordDeserializer.LanguageDeserializer.class)
    @Basic(fetch= FetchType.LAZY)
    public EmbeddedKeywordList languages = new EmbeddedKeywordList();
    
    @JSONEntity(title = "Naming Jurisdictions", format = "table", itemsTitle = "Jurisdiction", itemsFormat = JSONConstants.CV_JURISDICTION)
    @JsonSerialize(using= KeywordListSerializer.class)
    @JsonDeserialize(contentUsing= KeywordDeserializer.JurisdictionDeserializer.class)
    @Basic(fetch= FetchType.LAZY)
    public EmbeddedKeywordList nameJurisdiction = new EmbeddedKeywordList();
    
    @JSONEntity(title = "Naming Organizations", format = "table")

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    public List<NameOrg> nameOrgs = new ArrayList<NameOrg>();
    
    
    @JSONEntity(title = "Preferred Term")
    /**
     * There can be many preferred terms per substance
     */
    public boolean preferred;
    
    /**
     * There can only be 1 display name per substance
     */
    public boolean displayName;

    public Name () {}
    
    public Name (String name) {
    	this.name=name;
    }

    public String getName () {
    	return fullName != null ? fullName : name;
    }

    @PreUpdate
    private void preUpdate(){
    	
    	tidyName();
    	updateImmutables();
    }

    @PrePersist
    private void prePersist(){
    	tidyName();
    }

    private void tidyName () {
        if (HtmlUtil.isTruncatable(name)) {
            fullName = name;
            name = HtmlUtil.truncate(name);
        }
    }

    public void addLocator(Substance sub, String loc){
    	Reference r = new Reference();
    	r.docType=Name.SRS_LOCATOR;
    	r.citation=this.name + " [" + loc + "]";
    	r.publicDomain=true;
    	this.addReference(r,sub);
		// Turning this off; TagsValidator should do this work.
    	// 	sub.addTagString(loc);
    }
    
    /**
     * Returns the locators that have been added to this name record.
     * 
     * These are tags that are used for searching and display.
     * 
     * Currently, this requires the parent substance in order to 
     * make it work.
     * 
     * @param sub the parent substance of this name
     * @return
     */
    public List<String> getLocators(Substance sub){
    	List<String> locators = new ArrayList<String>();
    	//locators.add("TEST");
    	if(sub!=null){
    		//System.out.println("Real sub");
	    	for(Keyword ref: this.getReferences()){
	    		//System.out.println(ref.getValue());
	    		Reference r=sub.getReferenceByUUID(ref.getValue().toString());
	    		
	    		if(r!=null && r.docType!=null){
	    			//System.out.println(r.citation);
	    			
	    			if(r.docType.equals(Name.SRS_LOCATOR)){
	    				try{
	    					String tag=r.citation.split("\\[")[1].split("\\]")[0];
	    					locators.add(tag);
	    				}catch(Exception e){
	    					
	    				}
	    			}
	    		}
	    	}
    	}
    	return new ArrayList<String>(new TreeSet<String>(locators));
    }
    
    
    public static List<Name> sortNames(List<Name> nameList){
    	Collections.sort(nameList, Sorter.DISPlAY_NAME_FIRST_ENGLISH_FIRST);
    	return nameList;
    }
    
    
    public boolean isDisplayName() {
    	return displayName;
	}
    
    
    public void addLanguage(String lang){
    	if(!isLanguage(lang)){
    		this.languages.add(new Keyword(GinasCommonData.LANGUAGE,lang));
    	}
    }
    
    @JsonIgnore
	public boolean isOfficial() {
		if(this.type.equals("of"))return true;
		return false;
	}
    
	public boolean isLanguage(String lang){
		for(Keyword k:this.languages){
			if(k.getValue().equals(lang))return true;
		}
		return false;
	}
	
	

	public void updateImmutables(){
		super.updateImmutables();
		this.languages= new EmbeddedKeywordList(this.languages);
		this.domains= new EmbeddedKeywordList(this.domains);
		this.nameJurisdiction= new EmbeddedKeywordList(this.nameJurisdiction);
	}
	
	@Override
	public String toString(){
		return "Name{" +
				"name='" + name + '\'' +
				", fullName='" + fullName + '\'' +
				", stdName='" + stdName + '\'' +
				", type='" + type + '\'' +
				", domains=" + domains +
				", languages=" + languages +
				", nameJurisdiction=" + nameJurisdiction +
				", nameOrgs=" + nameOrgs +
				", preferred=" + preferred +
				", displayName=" + displayName +
				'}';
	}

	public void setName(String name) {
                this.fullName=null;
		this.name=name;
	}
	
	@Override
	@JsonIgnore
	public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences() {
		List<GinasAccessReferenceControlled> temp = new ArrayList<GinasAccessReferenceControlled>();
		if(this.nameOrgs!=null){
			for(NameOrg nos:this.nameOrgs){
				temp.addAll(nos.getAllChildrenAndSelfCapableOfHavingReferences());
			}
		}
		return temp;
	}

}
