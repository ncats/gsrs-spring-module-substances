package ix.ginas.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nih.ncats.common.sneak.Sneak;
import gov.nih.ncats.molwitch.Chemical;
import gsrs.json.JsonEntityUtil;
import ix.core.controllers.EntityFactory;
import ix.core.models.ParentReference;
import ix.core.models.Structure;
import ix.core.util.EntityUtils;
import ix.core.validator.GinasProcessingMessage;
import ix.ginas.models.v1.*;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by katzelda on 9/7/16.
 */
public class JsonSubstanceFactory {

    public static Substance makeSubstance(JsonNode tree){
        Substance s= internalMakeSubstance(tree, null);

        return JsonEntityUtil.fixOwners(s, true);
    }









    public static Substance makeSubstance(JsonNode tree, List<GinasProcessingMessage> messages) {
        return JsonEntityUtil.fixOwners(internalMakeSubstance(tree, messages), true);
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
                throw new IllegalStateException("JSON parse error:" + e.getMessage(), e);
            }
        } else {
            try {
                return mapper.treeToValue(tree, Substance.class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                throw new IllegalStateException("JSON parse error:" + e.getMessage(), e);

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
