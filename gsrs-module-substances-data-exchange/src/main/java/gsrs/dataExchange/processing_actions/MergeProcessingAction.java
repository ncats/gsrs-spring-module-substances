package gsrs.dataexchange.processing_actions;

import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.dataexchange.model.ProcessingAction;
import ix.core.util.EntityUtils;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class MergeProcessingAction implements ProcessingAction<Substance> {
    @Override
    public Substance process(Substance source, Substance existing, Map<String, Object> parameters, Consumer<String> processLog){
        SubstanceBuilder builder = existing.toBuilder();
        ObjectMapper mapper= new ObjectMapper();

        if(hasTrueValue(parameters, "MergeReferences")){
            source.references.forEach(r->{
                if( existing.references.stream().anyMatch(r2->r2.docType!=null && r2.docType.equals(r.docType) && r2.citation!=null
                        && r2.citation.equals(r.citation))){
                    processLog.accept(String.format("Reference %s/%s was already present;", r.docType, r.citation));
                } else {
                    EntityUtils.EntityInfo<Reference> eics= EntityUtils.getEntityInfoFor(Reference.class);
                    try {
                        Reference newReference= eics.fromJson(mapper.writeValueAsString(r));
                        builder.addReference(newReference);
                        processLog.accept(String.format("Reference %s/%s was copied", r.docType, r.citation));
                    }
                    catch (IOException e ) {
                        processLog.accept(String.format("Error copying reference %s/%s", r.docType, r.citation));
                        log.error("Error copying reference");
                    }
                }
            });
        }
        
        //todo: handle references within objects
        if(hasTrueValue(parameters, "MergeNames")){
            source.names.forEach(n-> {
                if( existing.names.stream().anyMatch(en->en.name.equals(n.name))){
                    processLog.accept(String.format("Name %s was already present;", n.name));
                }else{
                    EntityUtils.EntityInfo<Name> eics= EntityUtils.getEntityInfoFor(Name.class);
                    Name newName;
                    try {
                        newName = eics.fromJson(n.toJson());
                        builder.addName( newName);
                        processLog.accept(String.format("Adding Name %s;", n.name));
                    } catch (IOException e) {
                        log.error("error copying name", e);
                        processLog.accept(String.format("Error adding Name %s;", n.name));
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        if(hasTrueValue(parameters, "MergeCodes")){
            source.codes.forEach(c-> {
                if( existing.codes.stream().anyMatch(en->en.code.equals(c.code) && en.codeSystem.equals(c.codeSystem))){
                    processLog.accept(String.format("code %s was already present;", c.code));
                }else{
                    EntityUtils.EntityInfo<Code> eics= EntityUtils.getEntityInfoFor(Code.class);
                    try {
                        Code newCode=eics.fromJson(c.toJson());
                        builder.addCode( newCode);
                        processLog.accept(String.format("Adding code %s;", c.code));
                    } catch (IOException e) {
                        log.error("Error copying code", e);
                        processLog.accept(String.format("Error adding code %s of system %s;", c.code, c.codeSystem));
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        if(hasTrueValue(parameters, "MergeProperties")){
            source.properties.forEach(p-> {
                if( hasTrueValue(parameters, "PropertyNameUniqueness") &&
                        (existing.properties.stream().anyMatch(en->en.getPropertyType().equals(p.getPropertyType())
                        && en.getName().equals(p.getName()))
                        || builder.build().properties.stream().anyMatch(p2->p2.getName().equals(p.getName()) && p2.getPropertyType().equals(p.getPropertyType())))){
                    processLog.accept(String.format("property %s was already present;", p.getName()));
                }else{
                    EntityUtils.EntityInfo<Property> eics= EntityUtils.getEntityInfoFor(Property.class);
                    try {
                        Property newproperty=eics.fromJson(p.toJson());
                        builder.addProperty( newproperty);
                        processLog.accept(String.format("Adding property %s;", p.getName()));
                    } catch (IOException e) {
                        log.error("Error copying property", e);
                        processLog.accept(String.format("Error adding property %s;", p.getName()));
                        throw new RuntimeException(e);
                    }

                }
            });
        }
        return builder.build();
    }
}
