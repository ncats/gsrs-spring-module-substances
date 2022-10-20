package gsrs.module.substance.exporters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nih.ncats.molwitch.Chemical;
import ix.core.validator.GinasProcessingMessage;
import ix.ginas.exporters.Exporter;
import ix.ginas.exporters.ExporterFactory;
import ix.ginas.exporters.OutputFormat;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import lombok.Data;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by katzelda on 8/23/16.
 */
@Data
public class SdfExporterFactory implements ExporterFactory {

    private static final char NAME_PROP_SEPARATOR = '|';
    private static final Set<OutputFormat> formats = Collections.singleton( new OutputFormat("sdf", "SD (sdf) File"));
    private String approvalIDName ="APPROVAL_ID";
    public static final String NAME_PARAMETERS ="addNames";
    public static final String CODE_PARAMETERS ="addCodes";

    @Override
    public boolean supports(Parameters params) {
        return "sdf".equals(params.getFormat().getExtension());
    }

    @Override
    public Set<OutputFormat> getSupportedFormats() {
        return formats;
    }


    @Override
    public Exporter<Substance> createNewExporter(OutputStream out, Parameters params) throws IOException {
        return new SdfExporter(out, new BasicGsrsPropertyModifier(approvalIDName), params);
    }

    @Override
    public JsonNode getSchema() {
        ObjectNode parameters = JsonNodeFactory.instance.objectNode();
        parameters.put(NAME_PARAMETERS, "boolean");
        parameters.put(CODE_PARAMETERS, "boolean");
        parameters.put("approvalIDName", "string");
        return generateSchemaNode("SDF File Exporter Parameters", parameters);
    }

    /*public static void addProperties(Chemical c, Substance parentSubstance, List<GinasProcessingMessage> messages){
        new BasicGsrsPropertyModifier(approvalI ).modify(c, parentSubstance, messages);
    }

*/
    private class BasicGsrsPropertyModifier implements SdfExporter.ChemicalModifier {

        String approvalIdFieldName;
        private BasicGsrsPropertyModifier(String approvalIdFieldName) {
            this.approvalIdFieldName=approvalIdFieldName;
        }
        private void addNames(List<Name> srcNames, Substance parentSubstance, Set<String> destNames) {
            for (Name n : srcNames) {
                String name = n.name;

                StringBuilder propertyBuilder = new StringBuilder();
               String properties =  propertyBuilder.append(NAME_PROP_SEPARATOR)
                                .append(n.languages.stream().map(kw-> kw.getValue()).collect(Collectors.joining(",")))
                                .append(NAME_PROP_SEPARATOR)
//                                .append(n.displayName)
                                .toString();


                destNames.add(n.name + properties + n.displayName); // only include display name 1st time

                for (String loc : n.getLocators(parentSubstance)) {
                    //don't include display name for each locator because then we could have more than 1
                    destNames.add(name + " [" + loc + "]" + properties);
                }

            }
        }

        @Override
        public void modify(Chemical c, Substance parentSubstance, List<GinasProcessingMessage> messages,
                           JsonNode detailedParameters) {

            if (parentSubstance.approvalID != null) {
                c.setProperty(SdfExporterFactory.this.approvalIDName, parentSubstance.approvalID);
            }

            //TODO change names/name to have several name properties?
            //each Name is a separate field with lots of tabs
            //the new line separate properties
            //fields include language, and is display name

            c.setName(parentSubstance.getName());
            StringBuilder sb = new StringBuilder();

            if(detailedParameters!=null && detailedParameters.hasNonNull(NAME_PARAMETERS) && detailedParameters.get(NAME_PARAMETERS).booleanValue()) {
                c.setProperty("NAME", parentSubstance.getName());
                Set<String> names = new LinkedHashSet<>();
//            names.add(parentSubstance.getName());

                addNames(parentSubstance.getOfficialNames(), parentSubstance, names);
                addNames(parentSubstance.getNonOfficialNames(), parentSubstance, names);

                c.setProperty("NAMES", names.stream().collect(Collectors.joining("\n")));
            }


            if(detailedParameters!=null && detailedParameters.hasNonNull(CODE_PARAMETERS) && detailedParameters.get(CODE_PARAMETERS).booleanValue()) {
                //there's probably a way to make this into one giant collector...
                Map<String, List<Code>> codes = parentSubstance.codes.stream()
                        .collect(Collectors.groupingBy(cd -> cd.codeSystem));

                for (Map.Entry<String, List<Code>> entry : codes.entrySet()) {
                    c.setProperty(entry.getKey(), entry.getValue().stream()
                            .map(cd -> {
                                StringBuilder codeBuilder = new StringBuilder(cd.code);
                                if (!"PRIMARY".equals(cd.type)) {
                                    codeBuilder.append(" [").append(cd.type).append("]");
                                }
                                return codeBuilder.toString();
                            })
                            .distinct()
                            .collect(Collectors.joining("\n")));
                }
            }
            if (!messages.isEmpty()) {
                c.setProperty("EXPORT-WARNINGS", messages.stream()
                        .map(GinasProcessingMessage::getMessage)
                        .collect(Collectors.joining("\n")));

            }
        }

    }

}
