package ix.ginas.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import ix.core.models.Keyword;
import ix.core.models.Structure;
import ix.core.models.Value;
import ix.core.validator.GinasProcessingMessage;
import ix.ginas.models.GinasCommonSubData;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
@Slf4j

public class GinasV1ProblemHandler extends DeserializationProblemHandler {
        
		List<GinasProcessingMessage> messages = new ArrayList<GinasProcessingMessage>();
		
		public GinasV1ProblemHandler () {
        }
        public GinasV1ProblemHandler (List<GinasProcessingMessage> messages ) {
        	if(messages!=null)
        		this.messages=messages;
        }
        
        public boolean handleUnknownProperty
            (DeserializationContext ctx, JsonParser parser,
             JsonDeserializer deser, Object bean, String property) {

            try {
                boolean parsed = true;
                //removed hard-coded 'hash' -> 'lychi4'
                if ("count".equals(property)) {
                    if (bean instanceof Structure) {
                        // need to handle this.
                        parser.skipChildren();
                    }
                }
                else {
                    parsed = false;
                }

                if (!parsed) {
                	messages.add(GinasProcessingMessage.WARNING_MESSAGE("Unknown property \""
                            +property+"\" while parsing "
                            +bean+"; skipping it.."));
                	
                    log.debug("Unknown property \""
                                +property+"\" while parsing "
                                +bean+"; skipping it..");
                    log.debug("Token: "+parser.getCurrentToken());
                    parser.skipChildren();
                }
            }
            catch (Exception ex) {
            	messages.add(GinasProcessingMessage.ERROR_MESSAGE("Error parsing substance JSON:" + ex.getMessage()));
                ex.printStackTrace();
            }
            return true;
        }

        int parseReferences (JsonParser parser, List<Value> refs)
            throws IOException {
            int nrefs = 0;
            if (parser.getCurrentToken() == JsonToken.START_ARRAY) {
                while (JsonToken.END_ARRAY != parser.nextToken()) {
                    String ref = parser.getValueAsString();
                    refs.add(new Keyword(GinasCommonSubData.REFERENCE, ref));
                    ++nrefs;
                }
            }
            return nrefs;
        }        
    }