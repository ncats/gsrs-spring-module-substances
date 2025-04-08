package ix.core.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gov.nih.ncats.common.util.TimeUtil;
import gov.nih.ncats.molwitch.Bond;
import gov.nih.ncats.molwitch.Chemical;
import gov.nih.ncats.molwitch.inchi.Inchi;
import gov.nih.ncats.molwitch.io.CtTableCleaner;
import ix.core.AbstractValueDeserializer;
import ix.core.EntityMapperOptions;
import ix.core.chem.Chem;
import ix.core.chem.ChemCleaner;
import ix.core.validator.GinasProcessingMessage;
import ix.ginas.models.converters.StereoConverter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

//@MappedSuperclass
@Entity
@Inheritance
@DiscriminatorValue("DEF")
@Table(name = "ix_core_structure")
@Slf4j
public class Structure extends BaseModel {


    @Id
    @GenericGenerator(name = "NullUUIDGenerator", strategy = "ix.ginas.models.generators.NullUUIDGenerator")
    @GeneratedValue(generator = "NullUUIDGenerator")
    //maintain backwards compatibility with old GSRS store it as varchar(40) by basic hibernate will store uuids as binary
    @Type(type = "uuid-char" )
    @Column(length =40, updatable = false)
    public UUID id;

    @Version
    @JsonIgnore
    public Long version;
    @CreatedDate
    public Date created;
    @LastModifiedDate
    public Date lastEdited;
    public boolean deprecated;

    /**
     * Property labels
     */
    public static final String F_InChI = "InChI";
    public static final String F_MDL = "MDL";
    public static final String F_SMILES = "SMILES";
    public static final String F_MRV = "MRV";
    public static final String F_LyChI_SMILES = "LyChI_SMILES";
    public static final String H_LyChI_L1 = "LyChI_L1";
    public static final String H_LyChI_L2 = "LyChI_L2";
    public static final String H_LyChI_L3 = "LyChI_L3";
    public static final String H_LyChI_L4 = "LyChI_L4";
    public static final String H_InChI_Key = "InChI_Key";
    public static final String H_EXACT_HASH = "EXACT_HASH";
    public static final String H_STEREO_INSENSITIVE_HASH = "STEREO_INSENSITIVE_HASH";
    public static class StereoSerializer extends JsonSerializer<Stereo> {
    	public StereoSerializer(){
    		super();
    	}
        @Override
        public void serialize(Stereo value, JsonGenerator jgen, SerializerProvider provider)
          throws IOException, JsonProcessingException {
            jgen.writeString(value.stereoType);
        }
    }
    public static class StereoDeserializer extends JsonDeserializer<Stereo> {
    	 public StereoDeserializer(){
    		 super();
    	 }
        @Override
        public Stereo deserialize(JsonParser jp, DeserializationContext ctxt)
          throws IOException, JsonProcessingException {
            JsonNode node = jp.getCodec().readTree(jp);
            return new Stereo(node.asText());
        }
    }
   
    
    @JsonSerialize(using = StereoSerializer.class)
    @JsonDeserialize(using = StereoDeserializer.class)
    public static class Stereo {
        public static final Stereo ACHIRAL = new Stereo("ACHIRAL");
        public static final Stereo ABSOLUTE = new Stereo("ABSOLUTE");
        public static final Stereo RACEMIC = new Stereo("RACEMIC");
        public static final Stereo EPIMERIC = new Stereo("EPIMERIC");
        public static final Stereo MIXED = new Stereo("MIXED");
        public static final Stereo UNKNOWN = new Stereo("UNKNOWN");
        
		private String stereoType;

        public Stereo(String stereo){
        	this.stereoType=stereo;
        }

		public static Stereo valueOf(String asText) {
			return new Stereo(asText);
		}

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Stereo)) return false;
            Stereo stereo = (Stereo) o;
            return Objects.equals(stereoType, stereo.stereoType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(stereoType);
        }

        public String toString(){
			return this.stereoType;
		}
    }

    // optical activity
    public enum Optical {
        PLUS("( + )"),
        MINUS("( - )"),
        PLUS_MINUS("( + / - )"),
        UNSPECIFIED("UNSPECIFIED"),
        NONE("NONE");
        
        final String value;

        Optical(String value) {
            this.value = value;
        }

        @JsonValue
        public String toValue() {
            return value.toString();
        }

        @JsonCreator
        public static Optical forValue(String value) {
            if (value.equals("( + )") || value.equals("(+)"))
                return PLUS;
            if (value.equals("( - )") || value.equals("(-)"))
                return MINUS;
            if (value.equals("( + / - )") || value.equals("(+/-)"))
                return PLUS_MINUS;
            if (value.equalsIgnoreCase("unspecified"))
                return UNSPECIFIED;
            if (value.equalsIgnoreCase("none")
                || value.equalsIgnoreCase("unknown"))
                return NONE;
            return null;
        }
    }

    public enum NYU {
        No, Yes, Unknown
    }
    
    @Column(length = 128)
    public String digest; // digest checksum of the original structure
    
    @Lob
    @Basic(fetch = FetchType.EAGER)
    @Indexable(indexed = false)
    public String molfile;				

    @Lob
    @Basic(fetch = FetchType.EAGER)
    @Indexable(indexed = false)
    public String smiles;

    @Indexable(name = "Molecular Formula", facet = true)
    public String formula;

//    @JsonProperty("_formulaHTML")
//    public String getHtmlFormula() {
//        if (formula == null) {
//            return "";
//        }
//        String HTMLFormula = formula.replaceAll("([a-zA-Z])([0-9]+)", "$1<sub>$2</sub>");
//        if (charge != null && charge != 0 && !HTMLFormula.contains(".")) {
//            String sCharge = Integer.toString(charge);
//            String sSign = "+";
//            if (charge < 0) {
//                sCharge = sCharge.substring(1);
//                sSign = "-";
//            }
//            if ("1".equals(sCharge)) {
//                sCharge = "";
//            }
//            HTMLFormula = HTMLFormula + "<sup>" + sCharge + sSign + "</sup>";
//        }
//        return HTMLFormula;
//    }

    public void updateStructureFields(Structure other){
        if(other !=null) {
            this.properties.clear();

            this.properties = new ArrayList(other.properties); //add properties
            this.ezCenters = other.ezCenters;
            this.definedStereo = other.definedStereo;
            this.charge = other.charge;
            this.stereoCenters = other.stereoCenters;
            this.smiles = other.smiles;
            this.mwt = other.mwt;
            this.formula = other.formula;
            this.digest = other.digest;
            //TODO: only set as dirty if there's a real change.
            setIsDirty("properties");
            setIsDirty("ezCenters");
            setIsDirty("definedStereo");
            setIsDirty("charge");
            setIsDirty("stereoCenters");
            setIsDirty("mwt");
            setIsDirty("formula");
            setIsDirty("smiles");
            setIsDirty("digest");
        }
    }
    @JsonProperty("stereochemistry")
    public void setStereoChemistry(Stereo stereoChemistry) {
        this.stereoChemistry = stereoChemistry;
       
    }

    @JsonProperty("stereochemistry")
    @Indexable(name = "StereoChemistry", facet = true)
    @Column(name = "stereo" )
    @Convert(converter = StereoConverter.class)
    public Stereo stereoChemistry;
    
    
    @Column(name = "optical")
    public Optical opticalActivity;
    
    @Column(name = "atropi")
    @Enumerated(EnumType.ORDINAL)
    public NYU atropisomerism = NYU.No;
    
    @Lob
    @Basic(fetch = FetchType.EAGER)
    public String stereoComments;
    
    @Indexable(name = "Stereocenters", ranges = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 })
    public Integer stereoCenters; // count of possible stereocenters
    
    @Indexable(name = "Defined Stereocenters", ranges = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 })
    public Integer definedStereo; // count of defined stereocenters
    
    public Integer ezCenters; // counter of E/Z centers
    public Integer charge; // formal charge
    @Indexable(sortable=true)
    public Double mwt; // molecular weight

    @ManyToMany(cascade = CascadeType.ALL)
    @JsonView(BeanViews.JsonDiff.class)
    @JsonDeserialize(contentUsing=AbstractValueDeserializer.class)
    @EntityMapperOptions(linkoutInCompactView = true)
    @JoinTable(name="ix_core_structure_property",
        inverseJoinColumns = {@JoinColumn(name="ix_core_value_id")},
        indexes = {
            @Index(name="property_structure_id_index", columnList="ix_core_structure_id"),
            @Index(name="property_value_id_index", columnList="ix_core_value_id")}
    )
    public List<Value> properties = new ArrayList<Value>();

    @ManyToMany(cascade = CascadeType.ALL)
    @JsonView(BeanViews.JsonDiff.class)
    @EntityMapperOptions(linkoutInCompactView = true)
    @JoinTable(name="ix_core_structure_link", inverseJoinColumns = {
            @JoinColumn(name="ix_core_xref_id")
    })
    public List<XRef> links = new ArrayList<XRef>();

    @Transient
    private static ObjectMapper mapper = new ObjectMapper();

    public Integer count = 1; // moiety count?
    public Structure() {}
    
    
    public void setMwt(Double d){
    	this.mwt=Math.round(d* 10000.0)/10000.0;
    }


    public Double getMwt(){
    	if(this.mwt==null)return 0.0;

    	return Math.round(mwt* 10000.0)/10000.0;
    }




    @PrePersist
    @PreUpdate
    public void modified() {
        this.lastEdited = TimeUtil.getCurrentDate();
        if(atropisomerism==null){
            atropisomerism= NYU.No;
        }
//        System.out.println("before = "+ this.molfile);
        //GSRS-1515 clean up structure
        if(this.molfile !=null && !this.molfile.trim().isEmpty()){
            try {
                this.molfile = CtTableCleaner.clean(this.molfile);
            } catch (IOException e) {
               log.error("error cleaning mol file \n"+ this.molfile, e);
                //don't update it
            }
        }
//        System.out.println("after = "+ this.molfile);
    }


    public String getId() {
        return id != null ? id.toString() : null;
    }

    @JsonProperty(value="id")
    public UUID getRealId(){
    	return this.id;
    }

    @JsonProperty(value="hash")
    public String getExactHash(){
    	 String newhash=null;
    	 for (Value val : this.properties) {
	     if(val==null)continue;
             if (Structure.H_EXACT_HASH.equals(val.label)) {
            	 try{
            		 newhash=Objects.toString(val.getValue());
            	 }catch(Exception e){
            		 
            	 }
             }
         }
    	 return newhash;
    }
    
    @JsonIgnore
    public String getStereoInsensitiveHash(){
    	 String newhash=null;
    	 for (Value val : this.properties) {
	     if(val==null)continue;
             if (Structure.H_STEREO_INSENSITIVE_HASH.equals(val.label)) {
            	 try{
            		 newhash=Objects.toString(val.getValue());
            	 }catch(Exception e){
            		 
            	 }
             }
         }
    	 return newhash;
    }

	@Override
	public String fetchGlobalId() {
		if(this.id==null)return null;
		return id.toString();
	}

    //TODO katzelda Feb 2021 : this is done elsewhere in the springboot
//	@Override
//	public void forceUpdate() {
//		lastEdited=new Date();
//		super.save();
//	}
//
//	@Override
//	public boolean tryUpdate() {
//		long ov=version;
//		super.save();
//		return ov!=version;
//	}


    public void setId(UUID newid){
        if(this.id==null){
            this.id=newid;
        }
    }
    
    //TODO this is not used in the beta UI but saveTempStructure is called in the API via interpretStructure
    //so we will still have to support that
    /**
     * This is used to get a form of the structure which is guaranteed
     * to be present in an accessible way via ID. This is necessary only
     * when there is an ID which may not be present (anymore) in the database,
     * or not easily accessible due to some transient state.
     * @return
     */
    /*
    @JsonIgnore
    public Structure getDisplayStructure(){
    	Structure sfetch = StructureFactory.getStructure(this.id);
    	
    	if(sfetch==null || !sfetch.version.equals(this.version)){
    		try{
	    		Structure s= EntityWrapper.of(this).getClone();
	    		s.id = Util.sha1UUID(s.molfile+":" + s.digest);
	    		StructureFactory.saveTempStructure(s);
	    		return s;
    		}catch(Exception e){
    			log.error("Error saving display structure" , e);
    			StructureFactory.saveTempStructure(this);
    			return this;
    		}
    	}
    	return this;
    	
    }
    */

    @JsonIgnore
    @Transient
    public Chemical toChemical() {
        return toChemical(new ArrayList<>());
    }
    @JsonIgnore
    @Transient
    public Chemical toChemical(boolean computeCoordinatesIfNeeded) {
        return toChemical(new ArrayList<>(), computeCoordinatesIfNeeded);
    }

    
    @JsonProperty("_inchiKey")
    @Transient
    public String getInChIKey() {
    	try{
            for (Value val : this.properties) {
                if(val==null) continue;
                if (Structure.H_InChI_Key.equals(val.label)) {
                    return Objects.toString(val.getValue()).replaceAll("_", "-");                    
                }
            }
            return getInChIKeyAndThrow();
    	}catch(Exception e){
    		e.printStackTrace();
    		return null;
    	}
    }


    /*@JsonProperty("_inchiKeySet")
    @Transient
    public String getInChIKeySet() {
        try{
            if(this.stereoChemistry.equals(Stereo.RACEMIC) && this.opticalActivity.equals(Optical.PLUS_MINUS)) {
                this.toChemical().getExtendedTetrahedrals()
            }
            for (Value val : this.properties) {
                if(val==null) continue;
                if (Structure.H_InChI_Key.equals(val.label)) {
                    return Objects.toString(val.getValue()).replaceAll("_", "-");
                }
            }
            return getInChIKeyAndThrow();
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }*/

    @JsonIgnore
    @Transient
    public String getInChIKeyAndThrow() throws Exception{
       return Inchi.asStdInchi(Chem.RemoveQueryFeaturesForPseudoInChI(toChemical()), true).getKey();

    }

    @JsonProperty("_inchiKeySet")
    @JsonIgnore
    @Transient
    public List<String> getInChIKeysAndThrow() {

        log.trace("in getInChIKeysAndThrow(), stereoChemistry: {}", this.stereoChemistry);
        try {

        if( this.stereoChemistry == null || !(this.stereoChemistry.toString().equalsIgnoreCase(Stereo.EPIMERIC.toString())
            || this.stereoChemistry.toString().equalsIgnoreCase(Stereo.RACEMIC.toString() ))) {
            //handle non-epimers
            return Collections.singletonList(getInChIKey());
        }
        List<Chemical> epimers = toChemical().getImpl().permuteEpimersAndEnantiomers();
        return epimers.stream()
                .map(c -> {
                    try {
                        return Inchi.asStdInchi(Chem.RemoveQueryFeaturesForPseudoInChI(c), true).getKey();
                    } catch (IOException e) {
                        log.warn("Error calculating InChIKey", e);
                        return null;
                    }
                }).filter(i -> i != null)
                .collect(Collectors.toList());
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return Collections.singletonList("[Error]");
        }
    }

    @JsonProperty("_inchiSet")
    @JsonIgnore
    @Transient
    public List<String> getInChIsAndThrow() {

        log.trace("in getInChIsAndThrow(), stereoChemistry: {}", this.stereoChemistry);
        try {

            if( this.stereoChemistry == null || !(this.stereoChemistry.toString().equalsIgnoreCase(Stereo.EPIMERIC.toString())
                    || this.stereoChemistry.toString().equalsIgnoreCase(Stereo.RACEMIC.toString() ))) {
                //handle non-epimers
                return Collections.singletonList(getInChI());
            }
            List<Chemical> epimers = toChemical().getImpl().permuteEpimersAndEnantiomers();
            return epimers.stream()
                    .map(c -> {
                        try {
                            return Inchi.asStdInchi(Chem.RemoveQueryFeaturesForPseudoInChI(c), true).getInchi();
                        } catch (IOException e) {
                            log.warn("Error calculating InChI", e);
                            return null;
                        }
                    }).filter(i -> i != null)
                    .collect(Collectors.toList());
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return Collections.singletonList("[Error]");
        }
    }

    @JsonIgnore
    @Transient
    public String getInChIAndThrow() throws Exception{
        return Inchi.asStdInchi(Chem.RemoveQueryFeaturesForPseudoInChI(toChemical()), true).getInchi();

    }

    @JsonProperty("_inchi")
    @Transient
    public String getInChI() {
    	try{
            for (Value val : this.properties) {
                if(val==null) continue;
                if (Structure.F_InChI.equals(val.label)) {
                    return Objects.toString(val.getValue());
                }
            }
            return getInChIAndThrow();
    	}catch(Exception e){
    		e.printStackTrace();
    		return null;
    	}
    }
    
    @JsonIgnore
    @Transient
    public Chemical toChemical(List<GinasProcessingMessage> messages) {
        return toChemical(messages, true);
    }
    @JsonIgnore
    @Transient
    public Chemical toChemical(List<GinasProcessingMessage> messages, boolean computeCoordsIfNeeded) {
        Objects.requireNonNull(messages);
        Chemical c=null;
        String mfile = molfile;
        if(molfile ==null){
            Objects.requireNonNull(smiles);
            try {
                if(computeCoordsIfNeeded) {
                    c = Chemical.createFromSmilesAndComputeCoordinates(smiles);
                }else{
                    c = Chemical.createFromSmiles(smiles);
                }
            }catch(Exception e){
                e.printStackTrace();
                messages.add(GinasProcessingMessage.ERROR_MESSAGE("SMILES parsing error: %s", e.getMessage()));
            }
        
        }else{
            try {
                c = Chemical.parseMol(molfile);
            }catch(Exception e){
                e.printStackTrace();
                messages.add(GinasProcessingMessage.ERROR_MESSAGE("Molfile parsing error: %s", e.getMessage()));
            }
        }

        if(c==null || Chem.isProblem(c)){
        	messages.add(GinasProcessingMessage
                    .WARNING_MESSAGE("Structure format modified due to standardization"));
        	try {
                c = Chemical.parseMol(ChemCleaner.removeSGroupsAndLegacyAtomLists(mfile));

        	c.setProperty("WARNING", "Structure format modified due to standardization: removed SGROUPs");
            }catch(Exception e){
        	    throw new IllegalStateException("could not parse as mol/sdf", e);
            }
        }
        
        if (stereoChemistry != null)
            c.setProperty("STEREOCHEMISTRY", stereoChemistry.toString());
        if (opticalActivity != null)
            c.setProperty("OPTICAL_ACTIVITY", opticalActivity.toString());
        if (stereoComments != null)
            c.setProperty("STEREOCHEMISTRY_COMMENTS", stereoComments);
        if (stereoChemistry != null) {
            if(Structure.Stereo.EPIMERIC.equals(stereoChemistry) ||
                    Structure.Stereo.MIXED.equals(stereoChemistry) ||
                    Structure.Stereo.RACEMIC.equals(stereoChemistry) ||
                    Structure.Stereo.UNKNOWN.equals(stereoChemistry)) {
                messages.add(GinasProcessingMessage
                        .WARNING_MESSAGE("Structure format may not encode full stereochemical information"));
            }
        }
        if (smiles != null)
            c.setProperty("SMILES", smiles);
        return c;

    }


    
    //Supporting legacy endpoints for API for backwards compatibility
    @JsonProperty("stereoChemistry")
    @JsonView(BeanViews.Deprecated.class)
    @Deprecated
    public String getStereoChemistry() {
        if(this.stereoChemistry ==null){
            return null;
        }
        return this.stereoChemistry.toString();
    }

    private boolean hasAnyStereoBond() {
        return !this.toChemical().bonds().allMatch(b->b.getStereo().equals(Bond.Stereo.NONE));
    }

}
