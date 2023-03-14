package gsrs.module.substance.importers.importActionFactories;

import gsrs.dataexchange.model.MappingAction;
import gsrs.dataexchange.model.MappingActionFactoryMetadata;
import gsrs.dataexchange.model.MappingActionFactoryMetadataBuilder;
import gsrs.dataexchange.model.MappingParameter;
import gsrs.importer.PropertyBasedDataRecordContext;
import ix.ginas.modelBuilders.AbstractSubstanceBuilder;
import ix.ginas.modelBuilders.NucleicAcidSubstanceBuilder;
import ix.ginas.models.v1.Subunit;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static gsrs.module.substance.importers.importActionFactories.SubstanceImportAdapterFactoryBase.resolveParametersMap;

/*
Limitations:
1) a single subunit sequence
2) default sugar (dR for DNA and R for RNA)
3) default linkages (P)
 */
@Slf4j
public class SimpleNucleicAcidSequenceExtractorActionFactory extends BaseActionFactory{

    private String NucleicAcidType = "DNA";

    @Override
    public MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext> create(Map<String, Object> abstractParams) throws Exception {
        log.trace("in create");
        return (sub, dataRec) -> {
            if(! (sub instanceof NucleicAcidSubstanceBuilder)) {
                log.error("Error in create: substance is not a nucleic acid");
                return sub;
            }
            NucleicAcidSubstanceBuilder nucleicAcidSubstanceBuilder = (NucleicAcidSubstanceBuilder) sub;
            log.trace("lambda");
            abstractParams.keySet().forEach(k->log.trace("key: " + k + "; value: " +abstractParams.get(k)));
            Map<String, Object> params = resolveParametersMap(dataRec, abstractParams);
            log.trace("params: ");
            params.keySet().forEach(k->log.trace("key: " + k + "; value: " +abstractParams.get(k)));

            String sequenceRaw = (String) params.get("nucleicAcidSequence");
            /*if(subunitDelimiter!=null && subunitDelimiter.length()>0) {
                String[] sequences= sequenceRaw.split(subunitDelimiter);
                for(int s=0; s<sequences.length; s++){
                    Subunit subunit= new Subunit();
                    subunit.sequence=sequences[s];
                    subunit.subunitIndex=(s+1);//a guess
                    if( this.NucleicAcidType.equalsIgnoreCase("DNA")) {
                        nucleicAcidSubstanceBuilder.addDnaSubunit(sequences[s]);
                    }else {
                        nucleicAcidSubstanceBuilder.addRnaSubunit(sequences[s]);
                    }

                    log.trace("Added subunit with sequence {}", sequences[s]);
                }
            }  else {*/
                Subunit subunit= new Subunit();
                subunit.sequence=sequenceRaw;
                subunit.subunitIndex=1;//a guess
                String nucleicAcidType = (String) params.get("nucleicAcidType");
                if( nucleicAcidType.equalsIgnoreCase("DNA")) {
                    nucleicAcidSubstanceBuilder.addDnaSubunit(sequenceRaw);
                }else {
                    nucleicAcidSubstanceBuilder.addRnaSubunit(sequenceRaw);
                }

                log.trace("Added subunit with sequence {}", sequenceRaw);
            //}
            //doBasicsImports(c, params); -- not relevant to proteins?
            //TODO: consider more params

            return nucleicAcidSubstanceBuilder;
        };

    }

    @Override
    public MappingActionFactoryMetadata getMetadata() {
        MappingActionFactoryMetadataBuilder builder = new MappingActionFactoryMetadataBuilder();
        return builder.setLabel("Create Nucleic Acid Sequence")
                .addParameterField(MappingParameter.builder()
                        .setFieldName("nucleicAcidSequence")
                        .setValueType(String.class)
                        .setLabel("Nucleic Acid Sequence")
                        .setDefaultValue("NUCLEIC_ACID_SEQUENCE")
                        .setRequired(true)
                        .build())
                .addParameterField(MappingParameter.builder()
                        .setFieldName("nucleicAcidType")
                        .setValueType(String.class)
                        .setLabel("Nucleic Acid Type (DNA or RNA)")
                        .setDefaultValue("DNA")
                        .setRequired(true)
                        .build())
                .build();
    }
}
