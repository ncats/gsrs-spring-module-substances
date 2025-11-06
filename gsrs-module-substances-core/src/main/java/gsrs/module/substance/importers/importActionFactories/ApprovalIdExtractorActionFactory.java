package gsrs.module.substance.importers.importActionFactories;

import gsrs.dataexchange.model.MappingAction;
import gsrs.dataexchange.model.MappingActionFactoryMetadata;
import gsrs.dataexchange.model.MappingActionFactoryMetadataBuilder;
import gsrs.dataexchange.model.MappingParameter;
import gsrs.importer.PropertyBasedDataRecordContext;
import gsrs.repository.PrincipalRepository;
import gsrs.security.GsrsSecurityUtils;
import ix.ginas.modelBuilders.AbstractSubstanceBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import ix.core.models.Principal;

import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;

import static gsrs.module.substance.importers.importActionFactories.SubstanceImportAdapterFactoryBase.resolveParametersMap;

@Slf4j
public class ApprovalIdExtractorActionFactory extends BaseActionFactory{

    @Autowired
    private PrincipalRepository principalRepository;

    @Override
    public MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext> create(Map<String, Object> abstractParams) throws Exception {
        log.trace("in create");
        return (sub, sdRec) -> {
            log.trace("lambda");
            Map<String, Object> params = resolveParametersMap( sdRec, abstractParams);
            log.trace("params: ");
            params.keySet().forEach(k->log.trace("key: " + k + "; value: " +abstractParams.get(k)));
            String approvalIdValue =(String) params.get("approvalId");
            Matcher m = SubstanceImportAdapterFactoryBase.SDF_RESOLVE.matcher(approvalIdValue);
            if( approvalIdValue== null || approvalIdValue.length()==0 || (m.find() && approvalIdValue.equals(abstractParams.get("code") ))) {
                log.info("skipping blank approvalID");
                return sub;
            }
            Date approvalDate = new Date();
            if( abstractParams.containsKey("approvalDate") && abstractParams.get("approvalDate") instanceof Date ){
              approvalDate = (Date) abstractParams.get("approvalDate");
            } else if( params.containsKey("approvalDate") && params.get("approvalDate") instanceof Long) {
                approvalDate = new Date( (Long) params.get("approvalDate"));
            }
             String approver = (String)params.get("approver");
            if( approver == null || approver.length() == 0) {
                approver =  GsrsSecurityUtils.getCurrentUsername().isPresent() ? GsrsSecurityUtils.getCurrentUsername().get()
                    : null;
            }
            Principal approverPrincipal = approver!=null && approver.length() >0 ? principalRepository.findDistinctByUsernameIgnoreCase(approver)
                    : null;
            sub.setApproval(approverPrincipal, approvalDate, approvalIdValue);
            log.trace( "Added approval ID {}", approvalIdValue);
            return sub;
        };

    }

    @Override
    public MappingActionFactoryMetadata getMetadata() {
        MappingActionFactoryMetadataBuilder builder = new MappingActionFactoryMetadataBuilder();
        return builder.setLabel("Create Code")
                .addParameterField(MappingParameter.builder()
                        .setFieldName("approvalId")
                        .setValueType(String.class)
                        .setRequired(true)
                        .build())
                .addParameterField(MappingParameter.builder()
                        .setFieldName("approver")
                        .setValueType(String.class)
                        .setRequired(false)
                        .build())
                .addParameterField(MappingParameter.builder()
                        .setFieldName("approvalDate")
                        .setValueType(Date.class)
                        .setRequired(false)
                        .build())

                .build();
    }
}
