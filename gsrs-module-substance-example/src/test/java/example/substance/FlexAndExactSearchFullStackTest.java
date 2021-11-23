package example.substance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.ncats.molwitch.Chemical;
import gsrs.module.substance.controllers.SubstanceController;
import gsrs.module.substance.processors.RelationEventListener;
import ix.core.chem.InchiStandardizer;
import ix.core.chem.StructureStandardizer;
import ix.core.models.Structure;
import ix.ginas.modelBuilders.SubstanceBuilder;

@ActiveProfiles("test")
@RecordApplicationEvents
@Import({FlexAndExactSearchFullStackTest.Configuration.class, RelationEventListener.class})
@WithMockUser(username = "admin", roles="Admin")
public class FlexAndExactSearchFullStackTest  extends AbstractSubstanceJpaFullStackEntityTest {



    @Autowired
    protected SubstanceController substanceController;
    
    @Autowired
    protected StructureStandardizer standardizer;



    @TestConfiguration
    public static class Configuration{
//        @Value("${ix.core.structureIndex.atomLimit}")
//        private int maxNumberOfAtoms = 240;
//        @Value(value ="${ix.structure-standardizer}")
//        private Class<? extends AbstractStructureStandardizer> standardizerClass = InchiStandardizer.class;
        @Bean
        public StructureStandardizer getStructureStandardizer() throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
            InchiStandardizer istd = new InchiStandardizer();
            return new LoggingStructureStandardizer(istd);
        }
    }
    
    private static class LoggingStructureStandardizer implements StructureStandardizer{
        StructureStandardizer istd;
        
        int stdCallCount = 0;
        
        public LoggingStructureStandardizer(StructureStandardizer d) {
            istd=d;
        }
        
        @Override
        public String canonicalSmiles(Structure s, String mol) {
            return istd.canonicalSmiles(s, mol);
        }

        @Override
        public Chemical standardize(Chemical orig,
                Supplier<String> molSupplier,
                Consumer<ix.core.models.Value> valueConsumer)
                throws IOException {
            stdCallCount++;
            return istd.standardize(orig, molSupplier, valueConsumer);
        }
        
        public int getStdCallCount() {
            return this.stdCallCount;
        }
        
        public void reset() {
            this.stdCallCount=0;
        }
        
    }
    
    private static class MockRedirectAttributes implements RedirectAttributes{
        Map<String,Object> attributes = new LinkedHashMap<>();
        
        public Map<String,Object> getAttributes(){
            return this.attributes;
        }
        
        @Override
        public Model addAllAttributes(Map<String, ?> attributes) {

            return null;
        }

        @Override
        public boolean containsAttribute(String attributeName) {

            return false;
        }

        @Override
        public Object getAttribute(String attributeName) {

            return null;
        }

        @Override
        public Map<String, Object> asMap() {

            return null;
        }

        @Override
        public RedirectAttributes addAttribute(String attributeName,
                Object attributeValue) {

            attributes.put(attributeName,attributeValue);
            
            return null;
        }

        @Override
        public RedirectAttributes addAttribute(Object attributeValue) {

            return null;
        }

        @Override
        public RedirectAttributes addAllAttributes(
                Collection<?> attributeValues) {

            return null;
        }

        @Override
        public RedirectAttributes mergeAttributes(
                Map<String, ?> attributes) {

            return null;
        }

        @Override
        public RedirectAttributes addFlashAttribute(String attributeName,
                Object attributeValue) {

            return null;
        }

        @Override
        public RedirectAttributes addFlashAttribute(Object attributeValue) {

            return null;
        }

        @Override
        public Map<String, ?> getFlashAttributes() {

            return null;
        }
    }


    @Test
    public void ensureAFlexSearchForATempStoredStructureGetsStandardized() throws Exception {

        ObjectMapper om = new ObjectMapper();
        String smiles = "FC(F)(F)C(N(CCN2C(=O)C[C@H](N)CC(C(F)=CC3F)=CC(F)=3)C=1C2)=NN1";
        UUID uuid1 = UUID.randomUUID();
        new SubstanceBuilder()
        .asChemical()
        .setStructureWithDefaultReference(smiles)
        .addName("SITAGLIPTIN")
        .setUUID(uuid1)
        .buildJsonAnd(this::assertCreatedAPI);
        
        HttpServletRequest  mockedRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockedRequest.getRequestURI()).thenReturn("http://mock");
        
      
        
        
        MockRedirectAttributes mockAtt = new MockRedirectAttributes();
        
        LoggingStructureStandardizer lstd=(LoggingStructureStandardizer)standardizer;
            
        lstd.reset();
        assertEquals(0,lstd.getStdCallCount());
        
        ResponseEntity<Object> istruct = substanceController.interpretStructure(smiles, new HashMap<>());
                
        JsonNode jsn = om.readTree(istruct.getBody().toString());
        String strID=jsn.at("/structure/id").asText();        
        substanceController.structureSearchGet(strID, "exact", null, null, null, null, null, false, null, mockedRequest, mockAtt);
        
        //Should get called twice: once in full structure and once in moiety
        //TODO: eventually this may fail due to changing how processing works. 
        //Test should be rewritten accordingly
        
        
        assertEquals(2,lstd.getStdCallCount());
        assertEquals("root_structure_properties_term:MFFMDFFZMYYVKS_SECBINFHSA_N",mockAtt.getAttributes().get("q"));
    }
    
    

    @Test
    public void ensureAFlexSearchForADirectSmilesGetsStandardized() throws Exception {

        ObjectMapper om = new ObjectMapper();
        String smiles = "FC(F)(F)C(N(CCN2C(=O)C[C@H](N)CC(C(F)=CC3F)=CC(F)=3)C=1C2)=NN1";
        UUID uuid1 = UUID.randomUUID();
        new SubstanceBuilder()
        .asChemical()
        .setStructureWithDefaultReference(smiles)
        .addName("SITAGLIPTIN")
        .setUUID(uuid1)
        .buildJsonAnd(this::assertCreatedAPI);
        
        HttpServletRequest  mockedRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockedRequest.getRequestURI()).thenReturn("http://mock");
        
      
        
        
        MockRedirectAttributes mockAtt = new MockRedirectAttributes();
        
        LoggingStructureStandardizer lstd=(LoggingStructureStandardizer)standardizer;
            
        lstd.reset();
        assertEquals(0,lstd.getStdCallCount());
        
        substanceController.structureSearchGet(smiles, "exact", null, null, null, null, null, false, null, mockedRequest, mockAtt);
        

        assertTrue(lstd.getStdCallCount()>=2, "Standardization should be called at least twice, but could be called more in some cases");
        assertEquals("root_structure_properties_term:MFFMDFFZMYYVKS_SECBINFHSA_N",mockAtt.getAttributes().get("q"));
    }


}
