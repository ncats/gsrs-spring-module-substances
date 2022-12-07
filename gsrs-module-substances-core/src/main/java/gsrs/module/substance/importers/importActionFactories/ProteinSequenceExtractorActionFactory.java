package gsrs.module.substance.importers.importActionFactories;

import gsrs.dataexchange.model.MappingAction;
import gsrs.dataexchange.model.MappingActionFactoryMetadata;
import gsrs.dataexchange.model.MappingActionFactoryMetadataBuilder;
import gsrs.dataexchange.model.MappingParameter;
import gsrs.module.substance.importers.model.PropertyBasedDataRecordContext;
import ix.ginas.modelBuilders.AbstractSubstanceBuilder;
import ix.ginas.modelBuilders.ProteinSubstanceBuilder;
import ix.ginas.models.v1.Subunit;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static gsrs.module.substance.importers.SDFImportAdapterFactory.resolveParametersMap;

@Slf4j
@Data
public class ProteinSequenceExtractorActionFactory extends BaseActionFactory {

    private String sequenceField;

    private String subunitDelimiter;

    @Override
    public MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext> create(Map<String, Object> abstractParams) throws Exception {
        log.trace("in create");
        return (sub, dataRec) -> {
            if(! (sub  instanceof ProteinSubstanceBuilder)) {
                log.error("Error in create: substance is not a protein");
                return sub;
            }
            ProteinSubstanceBuilder proteinSubstance = (ProteinSubstanceBuilder) sub;
            //sub.setProtein(new Protein());
            //proteinSubstance.protein = ;
            log.trace("lambda");
            abstractParams.keySet().forEach(k->log.trace("key: " + k + "; value: " +abstractParams.get(k)));
            Map<String, Object> params = resolveParametersMap(dataRec, abstractParams);
            log.trace("params: ");
            params.keySet().forEach(k->log.trace("key: " + k + "; value: " +abstractParams.get(k)));

            String sequenceRaw = (String) params.get("proteinSequence");
            if(subunitDelimiter!=null && subunitDelimiter.length()>0) {
                String[] sequences= sequenceRaw.split(subunitDelimiter);
                for(int s=0; s<sequences.length; s++){
                    Subunit subunit= new Subunit();
                    subunit.sequence=sequences[s];
                    subunit.subunitIndex=(s+1);//a guess
                    proteinSubstance.addSubUnit(subunit).build().toBuilder();
                    log.trace("Added subunit with sequence {}", sequences[s]);
                }
            }  else {
                Subunit subunit= new Subunit();
                subunit.sequence=sequenceRaw;
                subunit.subunitIndex=1;//a guess
                proteinSubstance.addSubUnit(subunit).build().toBuilder();
                log.trace("Added subunit with sequence {}", sequenceRaw);
            }
            //doBasicsImports(c, params); -- not relevant to proteins?
            //TODO: consider more params

            return proteinSubstance;
        };

    }

    @Override
    public MappingActionFactoryMetadata getMetadata() {
        MappingActionFactoryMetadataBuilder builder = new MappingActionFactoryMetadataBuilder();
        return builder.setLabel("Create Protein Sequence")
                .addParameterField(MappingParameter.builder()
                        .setFieldName("sequenceFieldName")
                        .setValueType(String.class)
                        .setLabel("Sequence Field")
                        .setDefaultValue("PROTEIN_SEQUENCE")
                        .setRequired(true)
                        .build())
                .addParameterField(MappingParameter.builder()
                        .setFieldName("subunitDelimiter")
                        .setLabel("Delimiter (subunits)")
                        .setValueType(String.class)
                        .setDefaultValue("|")
                        .setRequired(true)
                        .build())
                .build();
    }

    @Override
    public void implementParameters() {
        if( this.parameters != null && !this.parameters.isEmpty()) {
            if( parameters.get("sequenceFieldName") != null ) {
                this.sequenceField = (String) parameters.get("sequenceFieldName");
            }
            if(parameters.get("subunitDelimiter") !=null) {
                this.subunitDelimiter = (String) parameters.get("subunitDelimiter");
            }
        }
    }

}
