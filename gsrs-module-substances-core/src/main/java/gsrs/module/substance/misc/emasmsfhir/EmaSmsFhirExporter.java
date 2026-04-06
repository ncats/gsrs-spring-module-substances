package gsrs.module.substance.misc.emasmsfhir;

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

    @Autowired
    private EmaSmsFhirConfiguration emaSmsFhirConfiguration;

    public EmaSmsFhirExporter(OutputStream os, ExporterFactory.Parameters params, String primaryCodeSystem) throws IOException{
        this.primaryCodeSystem = primaryCodeSystem;
        this.params = params;
        JsonNode detailedParameters = params.detailedParameters();

        omitPrimaryCodeSystem = (detailedParameters!=null
                && detailedParameters.hasNonNull(EmaSmsFhirExporterFactory.PRIMARY_CODE_SYSTEM_PARAMETERS)
                && detailedParameters.get(EmaSmsFhirExporterFactory.PRIMARY_CODE_SYSTEM_PARAMETERS).booleanValue());

        chosenApprovalIdName = (detailedParameters!=null
                && detailedParameters.hasNonNull(EmaSmsFhirExporterFactory.APPROVAL_ID_NAME_PARAMETERS)
                && detailedParameters.get(EmaSmsFhirExporterFactory.APPROVAL_ID_NAME_PARAMETERS).textValue().trim().length()>0)
                ? detailedParameters.get(EmaSmsFhirExporterFactory.APPROVAL_ID_NAME_PARAMETERS).textValue().trim() : EmaSmsFhirExporterFactory.DEFAULT_APPROVAL_ID_NAME;

        bw = new BufferedWriter(new OutputStreamWriter(os));

        StringBuilder sb = new StringBuilder();
//        if (null != emaSmsFhirConfiguration.getCodeConfigs()) {
            sb.append("testing: ").append( emaSmsFhirConfiguration.getCodeConfigs().toString());
//        }


//        StringBuilder sb = new StringBuilder();
//
//        sb.append("UUID").append("\t");
//        sb.append(chosenApprovalIdName).append("\t");
//        if(!omitPrimaryCodeSystem && primaryCodeSystem!=null) {
//            sb.append(primaryCodeSystem).append("\t");
//        }
//        sb.append("Code Public/Private").append("\t");
//        sb.append("CODE").append("\t");
//        sb.append("CODE_SYSTEM").append("\t");
//        sb.append("CODE_TYPE").append("\t");
//        sb.append("CODE_TEXT").append("\t");
//        sb.append("COMMENTS").append("\t");

        bw.write(sb.toString());
        bw.newLine();
    }

    @Override
    public void export(Substance obj) throws IOException {
        // The substance corresponds to one line of "scrubbed" data.
        String priCode = null;
        if(!omitPrimaryCodeSystem && primaryCodeSystem!=null) {
            priCode = obj.codes.stream().filter(cd -> cd.codeSystem.equals(primaryCodeSystem)
                    && cd.type.equals("PRIMARY")).map(cd -> cd.code).findFirst().orElse(null);
        }
        String uuid = obj.getUuid().toString();
        for (Code c : obj.getCodes()) {
            StringBuilder sb = new StringBuilder();
            sb.append(uuid).append("\t");
            sb.append(obj.approvalID).append("\t");
            if(!omitPrimaryCodeSystem && primaryCodeSystem!=null) {
                sb.append(priCode).append("\t");
            }
            sb.append(
                    (c.getAccess().isEmpty())
                            ? "Public" : "Private: " + ExporterUtilities.makeAccessGroupString(c.getAccess())).append("\t");
            sb.append(c.code).append("\t");
            sb.append(c.codeSystem).append("\t");
            sb.append(c.type).append("\t");
            sb.append((c.codeText != null) ? c.codeText : "").append("\t");
            sb.append((c.comments != null) ? ExporterUtilities.replaceAllLinefeedsWithPipes(c.comments) : "");
            bw.write(sb.toString());
            bw.newLine();
        }
    }

    @Override
    public void close() throws IOException {
        bw.close();
    }
}
