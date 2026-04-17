package gsrs.module.substance.misc.emasmsfhir;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectWriter;
import ix.core.controllers.EntityFactory;
import ix.ginas.exporters.Exporter;
import ix.ginas.exporters.ExporterFactory;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;


@Slf4j
public class EmaSmsFhirExporter implements Exporter<Substance> {

    private final BufferedWriter bw;
    private final String primaryCodeSystem;

    private ExporterFactory.Parameters params;
    private boolean omitPrimaryCodeSystem;
    private String chosenApprovalIdName;
    private EmaSmsFhirConfiguration emaSmsFhirConfiguration;
    private EmaSmsSubstanceDefinitionFhirMapper emaSmsSubstanceDefinitionFhirMapper;


    public EmaSmsFhirExporter(OutputStream os, ExporterFactory.Parameters params, String primaryCodeSystem, EmaSmsFhirConfiguration emaSmsFhirConfiguration, EmaSmsSubstanceDefinitionFhirMapper emaSmsSubstanceDefinitionFhirMapper) throws IOException{
        this.primaryCodeSystem = primaryCodeSystem;
        this.params = params;
        this.emaSmsFhirConfiguration = emaSmsFhirConfiguration;
        this.emaSmsSubstanceDefinitionFhirMapper = emaSmsSubstanceDefinitionFhirMapper;

        JsonNode detailedParameters = params.detailedParameters();

        omitPrimaryCodeSystem = (detailedParameters!=null
                && detailedParameters.hasNonNull(EmaSmsFhirExporterFactory.PRIMARY_CODE_SYSTEM_PARAMETERS)
                && detailedParameters.get(EmaSmsFhirExporterFactory.PRIMARY_CODE_SYSTEM_PARAMETERS).booleanValue());

        chosenApprovalIdName = (detailedParameters!=null
                && detailedParameters.hasNonNull(EmaSmsFhirExporterFactory.APPROVAL_ID_NAME_PARAMETERS)
                && detailedParameters.get(EmaSmsFhirExporterFactory.APPROVAL_ID_NAME_PARAMETERS).textValue().trim().length()>0)
                ? detailedParameters.get(EmaSmsFhirExporterFactory.APPROVAL_ID_NAME_PARAMETERS).textValue().trim() : EmaSmsFhirExporterFactory.DEFAULT_APPROVAL_ID_NAME;

        bw = new BufferedWriter(new OutputStreamWriter(os));
//        StringBuilder sb = new StringBuilder();
//        bw.write(sb.toString());
//        bw.newLine();
    }
    FhirContext ctx = FhirContext.forR5();

    @Override
    public void export(Substance obj) throws IOException {
        // The substance corresponds to one line of "scrubbed" data.

        StringBuilder sb = new StringBuilder();
        bw.write(
            ctx.newJsonParser().setPrettyPrint(false).encodeResourceToString(
              emaSmsSubstanceDefinitionFhirMapper.generateEmaSmsSubstanceDefinitionFromSubstance(obj)
            )
        );
        bw.newLine();


    }

    @Override
    public void close() throws IOException {
        bw.close();
    }
}
