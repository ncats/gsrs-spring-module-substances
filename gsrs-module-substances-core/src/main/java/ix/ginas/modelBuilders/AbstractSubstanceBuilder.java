package ix.ginas.modelBuilders;

import com.fasterxml.jackson.databind.JsonNode;
import ix.core.controllers.EntityFactory;
import ix.core.models.Group;
import ix.core.models.Keyword;
import ix.core.models.Principal;
import ix.ginas.models.v1.*;
import ix.ginas.models.v1.Substance.SubstanceClass;
import ix.ginas.models.v1.Substance.SubstanceDefinitionLevel;
import ix.ginas.models.v1.Substance.SubstanceDefinitionType;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class AbstractSubstanceBuilder<S extends Substance, T extends AbstractSubstanceBuilder<S,T>>{
	
	Function<S, S> andThen = (f->f);
	
	public abstract Supplier<S> getSupplier();

	protected abstract Substance.SubstanceClass getSubstanceClass();

	protected abstract T getThis();


    public AbstractSubstanceBuilder(){
    }

    public AbstractSubstanceBuilder(Substance copy){

        UUID uuid = copy.getUuid();
        if(uuid !=null){
            setUUID(uuid);
        }

        for(Name name: copy.names){
            addName(name);
        }

        for(Code code : copy.codes){
            addCode(code);
        }

        for(Reference r : copy.references){
            addReference(r);
        }
        for(Note n : copy.notes){
            addNote(n);
        }

        for(Property p : copy.properties){
            addProperty(p);
        }
        for(Relationship r : copy.relationships){
            addRelationship(r);
        }

        for(Keyword k : copy.tags){
            addKeyword(k);
        }

        setDefinition(copy.definitionType, copy.definitionLevel);
        Substance.SubstanceClass substanceClass = copy.substanceClass;
        if(substanceClass !=null){
            setSubstanceClass(substanceClass);
        }

        setStatus(copy.status);

        setVersion(Integer.parseInt(copy.version));

        if(copy.approvalID !=null){
            setApproval(copy.approvedBy, copy.approved, copy.approvalID);
        }

        if(copy.changeReason !=null){
            setChangeReason(copy.changeReason);
        }

        if(copy.modifications !=null){
            setModifications(copy.modifications);
        }
        
        
        if(copy.lastEdited !=null){
        	setLastEditedDate(copy.lastEdited);
        }
        if(copy.created !=null){
        	setCreatedDate(copy.created);
        }
        
        if(copy.createdBy !=null){
        	setCreatedBy(copy.createdBy);
        }
        if(copy.lastEditedBy !=null){
        	setLastEditedBy(copy.lastEditedBy);
        }
    }

    private T setModifications(Modifications modifications) {
        return andThen( s->{
            s.modifications = modifications;
        });
    }

    private T setChangeReason(String changeReason) {
        return andThen( s->{
            s.changeReason = changeReason;
        });
    }

    public T setApproval(Principal approvedBy, Date approved, String approvalID) {
        return andThen(s ->{
            s.approvalID = approvalID;
            s.approved = approved;
            s.approvedBy = approvedBy;
        });
    }

    public T setCreatedBy(Principal p){
        return andThen( s->{s.createdBy = p;});
    }

    public T setLastEditedBy(Principal p){
        return andThen( s->{s.lastEditedBy = p;});
    }
    
    public T setCreatedDate(Date d){
        return andThen( s->{s.setCreated(d);});
    }
    
    public T setLastEditedDate(Date d){
        return andThen( s->{s.setLastEdited(d);});
    }
    
    public T addReflexiveActiveMoietyRelationship(){
    	return andThen( s->{ 
    		Relationship r = new Relationship();
    		r.type= Relationship.ACTIVE_MOIETY_RELATIONSHIP_TYPE;
    		r.relatedSubstance=s.asSubstanceReference();
    		s.relationships.add(r);
    	});
    }
    
    
    public T setVersion(int version){
        return andThen( s->{ s.version = Integer.toString(version);});
    }
    public T setStatus(String status){
        return andThen( s->{ s.status = status;});
    }
    public T setDefinition(Substance.SubstanceDefinitionType type, Substance.SubstanceDefinitionLevel level) {
        return andThen(s -> {
            s.definitionType = type;
            s.definitionLevel = level;
        });
    }
    public T setSubstanceClass(Substance.SubstanceClass c) {
        return andThen(s -> {s.substanceClass = c;});
    }
    public T addNote(Note n) {
        return andThen(s -> {s.notes.add(n);});
    }

    public T addReference(Reference r) {
        return andThen(s -> {s.references.add(r);});
    }

    public T onSubstanceClass(BiConsumer<Substance.SubstanceClass, S> consumer){
        return andThen( s-> {consumer.accept(s.substanceClass, s);});
    }
    public T addRelationship(Relationship r) {
        return andThen(s -> {s.relationships.add(r);});
    }
    
    public T clearRelationships() {
        return andThen(s -> {s.relationships.clear();});
    }

    public T addKeyword(Keyword k) {
        return andThen(s -> {s.tags.add(k);});
    }


    public T addProperty(Property p) {
        return andThen(s -> {
            if(p!=null) {
                s.properties.add(p);
                p.setOwner(s);
            }
        });
    }
    public T addName(Name name) {
        return andThen(s -> {
            s.names =  addToNewList(s.names, name);
        });
    }

    protected static Reference createNewPublicDomainRef(){
        Reference r = new Reference();
        r.publicDomain = true;
        r.setAccess(Collections.emptySet());
        r.addTag(Reference.PUBLIC_DOMAIN_REF);

        return r;
    }

    private <T> List<T> addToNewList(List<T> oldList, T newElement){
        List<T> newList;
        if(oldList ==null){
            newList = new ArrayList<>();
        }else {
            newList = new ArrayList<>(oldList);
        }
        newList.add(newElement);
        return newList;
    }

    public T addCode(Code code) {
        return andThen(s ->{
            if(code.getReferences().isEmpty()) {
                code.addReference(getOrAddFirstReference(s) ,s);
            }
            s.codes.add(code);
        });
    }

    public T andThen(Function<S, S> fun){
		andThen = andThen.andThen(fun);
		return getThis();
	}

    public T andThen(Consumer<S> fun){
        andThen = andThen.andThen(s ->{ fun.accept(s); return s;});
        return getThis();
    }
    
    public T andThenMutate(Consumer<S> fun){
        andThen = andThen.andThen(s ->{ fun.accept(s); return s;});
        return getThis();
    }
	
	public Supplier<S> asSupplier(){
		return (()->afterCreate().apply(getSupplier().get()));
	}
	
	public final Function<S, S> afterCreate(){
		return andThen;
	}

    public T addName(String name, Set<Group> access){
       return createAndAddBasicName(name, n-> n.setAccess(access));
    }


    private Name createName(Substance s, String name){
        Name n=new Name(name);
//        n.addLanguage("en");
        n.addReference(getOrAddFirstReference(s), s);
        return n;
    }
    private T createAndAddBasicName(String name){
        return createAndAddBasicName(name, (BiConsumer<S, Name>)null);
    }
    private T createAndAddBasicName(String name, BiConsumer<S, Name> additionalNameOpperations) {
        return andThen(s->{


            Name n = createName(s, name);
            if(additionalNameOpperations !=null){
                additionalNameOpperations.accept(s, n);
            }
            s.names =  addToNewList(s.names, n);
        });
    }

    private T createAndAddBasicName(String name, Consumer<Name> additionalNameOpperations){
       return createAndAddBasicName(name, (s,n)-> additionalNameOpperations.accept(n));
    }

    public T addName(String name, Consumer<Name> additionalNameOpperations){
        return createAndAddBasicName(name, additionalNameOpperations);
    }
	public T addName(String name){
        return createAndAddBasicName(name);
	}
	
	public T addCode(String codeSystem, String code){
		return andThen(s->{
			Code c=new Code(codeSystem,code);
			c.addReference(getOrAddFirstReference(s), s);
			s.codes.add(c);
		});
	}
	
	//Helper internal thing
	public static Reference getOrAddFirstReference(Substance s){

			Reference rr= Reference.SYSTEM_GENERATED();
			rr.publicDomain=true;
			rr.addTag(Reference.PUBLIC_DOMAIN_REF);
			rr.getOrGenerateUUID();
			return rr;

	}

	public S build(){

	    S s = getSupplier().get();
	    s.substanceClass = getSubstanceClass();
	    return additionalSetup().apply(afterCreate().apply(s));
	}

	public  Function<S, S> additionalSetup(){
	    return Function.identity();
    }
	
	public JsonNode buildJson(){
		return EntityFactory.EntityMapper.FULL_ENTITY_MAPPER().valueToTree(this.build());
	}
	
	public void buildJsonAnd(Consumer<JsonNode> c){
		c.accept(buildJson());
	}


    public T setUUID(UUID uuid) {
       return andThen(s -> {s.uuid=uuid;});
    }



    public T removeUUID(){
        return andThen( s-> {s.uuid =null;});
    }

    public T generateNewUUID(){
        return andThen( s-> {
            s.uuid =null;
            s.getOrGenerateUUID();});
    }

    public T setDisplayName(String displayName) {
        return createAndAddBasicName(displayName, (substance, name) -> {

            for(Name n : substance.names){
                n.displayName=false;
            }
            name.displayName = true;
        });

    }

    public T removeCodes(java.util.function.Predicate<Code> test) {
        Objects.requireNonNull(test);
        return andThen(s -> {
            s.codes = s.getCodes().stream().filter(test.negate()).collect(Collectors.toList());
        });
    }

    public T modifyNames(Consumer<List<Name>> namesConsumer){
        Objects.requireNonNull(namesConsumer);
        return andThen( s-> {namesConsumer.accept(s.names);});
    }

    public T setAccess(Set<Group> groups){
        return andThen( s-> {s.setAccess(groups);});
    }

    public T setToPublic(){
        return setAccess(Collections.emptySet());
    }

    public T addRelationshipTo(Substance relatedSubstance, String type){
        Relationship rel = new Relationship();
        rel.type = type;
        rel.relatedSubstance = relatedSubstance.asSubstanceReference();
        return addRelationship(rel);
    }

    public T addActiveMoiety(){
        return andThen( s->{
            Relationship rel = new Relationship();
            rel.type = Relationship.ACTIVE_MOIETY_RELATIONSHIP_TYPE;
            rel.relatedSubstance = s.asSubstanceReference();

            s.relationships.add(rel);
        });
    }
    
    public T makeAlternativeFor(
            Substance sub1Fetched) {
        return this.clearRelationships()
                   .addRelationshipTo(sub1Fetched, Substance.PRIMARY_SUBSTANCE_REL)
                   .setDefinition(SubstanceDefinitionType.ALTERNATIVE, SubstanceDefinitionLevel.COMPLETE);
    }
    
    public SubstanceBuilder asConcept(){
        return new SubstanceBuilder(this).setSubstanceClass(SubstanceClass.concept);
    }
}