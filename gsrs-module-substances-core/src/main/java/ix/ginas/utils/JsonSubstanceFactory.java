package ix.ginas.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nih.ncats.molwitch.Chemical;
import gsrs.module.substance.SubstanceOwnerReference;
import ix.core.controllers.EntityFactory;
import ix.core.models.Structure;
import ix.core.util.EntityUtils;
import ix.core.validator.GinasProcessingMessage;
import ix.ginas.models.v1.*;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by katzelda on 9/7/16.
 */
public class JsonSubstanceFactory {

    public static Substance makeSubstance(JsonNode tree){
        Substance s= internalMakeSubstance(tree, null);

        return fixOwners(s);
    }

    private static void setOwners(Substance substance, Object obj, boolean force) throws IllegalAccessException {
        Class<?> aClass = obj.getClass();
        do {
            setOwner(substance, obj, aClass, force);
            aClass = aClass.getSuperclass();
        }while(aClass !=null);
    }

    private static void setOwner(Substance substance, Object obj, Class<?> aClass, boolean force) throws IllegalAccessException {
        for(Field f : aClass.getDeclaredFields()){
            f.setAccessible(true);
            if(f.getAnnotation(SubstanceOwnerReference.class) !=null){
                if(force || f.get(obj) == null){
                    if(obj instanceof Relationship){
                        System.out.println("here");
                    }
                    f.set(obj, substance);
                }
            }
        }
    }

    public static Substance fixOwners(Substance s){
        return fixOwners(s, false);
    }
    public static Substance fixOwners(Substance s, boolean force){
        return fixOwners(s,s,force);
    }
    public static Substance fixOwners(Substance substanceToModify, Substance ownerSubstance, boolean force){
        if(substanceToModify ==null){
            return null;
        }
        Map<Integer, EntityUtils.EntityWrapper> map = new HashMap<>();
        EntityUtils.EntityWrapper.of(substanceToModify).traverse().execute((path, e)->{
            Object o =e.getRawValue();
            if(o !=null){
                try {
                    setOwners(ownerSubstance, o, force);
                } catch (IllegalAccessException ex) {
                    ex.printStackTrace();
                }
            }
        });

        //TODO add others
        return substanceToModify;
    }
    public static Substance makeSubstance(JsonNode tree, List<GinasProcessingMessage> messages) {
        return fixOwners(internalMakeSubstance(tree, messages));
    }
    public static Substance internalMakeSubstance(JsonNode tree, List<GinasProcessingMessage> messages) {

        JsonNode subclass = tree.get("substanceClass");
        ObjectMapper mapper = EntityFactory.EntityMapper.FULL_ENTITY_MAPPER();

        mapper.addHandler(new GinasV1ProblemHandler(messages));
        Substance sub = null;
        if (subclass != null && !subclass.isNull()) {

            Substance.SubstanceClass type;
            try {
                type = Substance.SubstanceClass.valueOf(subclass.asText());
            } catch (Exception e) {
                throw new IllegalStateException("Unimplemented substance class:" + subclass.asText());
            }
            try {
                switch (type) {
                    case chemical:

                        ObjectNode structure = (ObjectNode)tree.at("/structure");
                        fixStereoOnStructure(structure);
                        for(JsonNode moiety: tree.at("/moieties")){
                            fixStereoOnStructure((ObjectNode)moiety);
                        }

                        sub = mapper.treeToValue(tree, ChemicalSubstance.class);


                        try {
                            ((ChemicalSubstance) sub).getStructure().smiles = Chemical.parseMol(((ChemicalSubstance) sub).getStructure().molfile).toSmiles();
                        } catch (Exception e) {

                        }

                        return sub;
                    case protein:
                        sub = mapper.treeToValue(tree, ProteinSubstance.class);
                        return sub;
                    case mixture:
                        sub = mapper.treeToValue(tree, MixtureSubstance.class);
                        return sub;
                    case nucleicAcid:
                        sub = mapper.treeToValue(tree, NucleicAcidSubstance.class);
                        return sub;
                    case polymer:
                        sub = mapper.treeToValue(tree, PolymerSubstance.class);
                        return sub;
                    case structurallyDiverse:
                        sub = mapper.treeToValue(tree, StructurallyDiverseSubstance.class);
                        return sub;
                    case specifiedSubstanceG1:
                        sub = mapper.treeToValue(tree, SpecifiedSubstanceGroup1Substance.class);
                        return sub;
                    case concept:
                        sub = mapper.treeToValue(tree, Substance.class);
                        return sub;
                    default:
                        throw new IllegalStateException(
                                "JSON parse error: Unimplemented substance class:\"" + subclass.asText() + "\"");
                }
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                System.err.println(tree.toPrettyString());
                throw new IllegalStateException("JSON parse error:" + e.getMessage());
            }
        } else {
            try {
                return mapper.treeToValue(tree, Substance.class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                throw new IllegalStateException("JSON parse error:" + e.getMessage());

            }
//            throw new IllegalStateException("Not a valid JSON substance! \"substanceClass\" cannot be null!");
        }
    }

    private static void fixStereoOnStructure(ObjectNode structure){
        JsonNode jsn=structure.at("/stereochemistry");
        try{
            Structure.Stereo str= Structure.Stereo.valueOf(jsn.asText());
        }catch(Exception e){
            //e.printStackTrace();
            //System.out.println("Unknown stereo:'" + jsn.asText() + "'");
            if(!jsn.asText().equals("")){
                //System.out.println("Is not nothin");
                String newStereo=jsn.toString();
                JsonNode oldnode=structure.get("stereocomments");

                if(oldnode!=null && !oldnode.isNull() && !oldnode.isMissingNode() &&
                        !oldnode.toString().equals("")){
                    newStereo+=";" +oldnode.toString();
                }
                structure.put("stereocomments",newStereo);
                structure.put("atropisomerism", "Yes");

            }
            structure.put("stereochemistry", "UNKNOWN");
        }
    }
}
