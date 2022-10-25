package ix.ginas.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import gov.nih.ncats.molwitch.Chemical;
import gsrs.json.JsonEntityUtil;
import ix.core.controllers.EntityFactory;
import ix.core.models.Structure;
import ix.core.validator.GinasProcessingMessage;
import ix.ginas.models.v1.*;

import java.util.List;
import java.util.UUID;

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
    public static Class<? extends Substance> getSubstanceKind(JsonNode tree){
        JsonNode subclass = tree.get("substanceClass");
        if (subclass != null && !subclass.isNull()) {
            Substance.SubstanceClass type;
            try {
                type = Substance.SubstanceClass.valueOf(subclass.asText());
            } catch (Exception e) {
                throw new IllegalStateException("Unimplemented substance class:" + subclass.asText());
            }
                switch (type) {
                    case chemical:
                        return ChemicalSubstance.class;
                    case concept:
                        return Substance.class;
                    case mixture:
                        return MixtureSubstance.class;
                    case nucleicAcid:
                        return NucleicAcidSubstance.class;
                    case protein:
                        return ProteinSubstance.class;
                    case polymer:
                        return PolymerSubstance.class;
                    case specifiedSubstanceG1:
                        return SpecifiedSubstanceGroup1Substance.class;
                    case structurallyDiverse:
                        return StructurallyDiverseSubstance.class;
                    default:   throw new IllegalStateException(
                            "JSON parse error: Unimplemented substance class:\"" + subclass.asText() + "\"");
                }


        }
        return Substance.class;
    }
    public static Substance internalMakeSubstance(JsonNode tree, List<GinasProcessingMessage> messages) {

        fixReferences(tree);
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

    private static void fixReferences(JsonNode tree) {
        ArrayNode references = (ArrayNode)tree.at("/references");
        for (int i = 0; i < references.size(); i++) {
            ObjectNode ref = (ObjectNode) references.get(i);
            if (!ref.has("uuid")) {
                ref.set("uuid", new TextNode(UUID.randomUUID().toString()));
            }
        }
        for (JsonNode refsNode: tree.findValues("references")) {
            if (refsNode.isArray()) {
                ArrayNode refs = (ArrayNode) refsNode;
                for (int i = 0; i < refs.size(); i++) {
                    JsonNode ref = refs.get(i);
                    if (ref.isTextual() && ref.asText("").chars().allMatch(Character::isDigit)) {
                        refs.set(i, references.get(ref.asInt()).get("uuid"));
                    }
                }
            }
        }
    }
}
