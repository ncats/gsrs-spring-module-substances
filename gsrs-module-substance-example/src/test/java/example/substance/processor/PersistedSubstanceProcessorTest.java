package example.substance.processor;

import example.substance.AbstractSubstanceJpaEntityTest;

import gsrs.repository.EditRepository;
import gsrs.startertests.TestEntityProcessorFactory;
import ix.core.EntityProcessor;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;


import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by katzelda on 12/19/17.
 */
@Import({PersistedSubstanceProcessorTest.PersistedSubstanceProcessorTestDouble.class})
public class PersistedSubstanceProcessorTest extends AbstractSubstanceJpaEntityTest {


    @Autowired
    private TestEntityProcessorFactory entityProcessorFactory;
    @Autowired
    private PersistedSubstanceProcessorTestDouble persistedSubstanceProcessorTestDouble;
    @BeforeEach
    public void addEntityProcessor(){
        entityProcessorFactory.setEntityProcessors(persistedSubstanceProcessorTestDouble);
        persistedSubstanceProcessorTestDouble.reset();
    }

    @Test
    @WithMockUser(username = "admin", roles="Admin")
    public void newSubstanceShouldCallNew() {


            new SubstanceBuilder()
                    .addName("aName")
                    .buildJsonAnd( this::assertCreated);

            assertEquals(1, PersistedSubstanceProcessorTestDouble.timesNewCalled);
            assertEquals(0, PersistedSubstanceProcessorTestDouble.timesUpdatedCalled);
//            assertNotNull(PersistedSubstanceProcessorTestDouble.lastNewSubstance);




    }

    @Test
    @WithMockUser(username = "admin", roles="Admin")
    public void update() {

        UUID uuid = UUID.randomUUID();

            new SubstanceBuilder()
                    .addName("aName")
                    .setUUID(uuid)
                    .buildJsonAnd(this::assertCreated);

            System.out.println("=======================================");


            substanceEntityService.get(uuid).get().toBuilder()
                    .addName("secondNme")
//                            .setVersion(2)
                    .buildJsonAnd(this::assertUpdated);




            assertEquals(new HashSet<>(Arrays.asList("aName", "secondNme")),
                    substanceEntityService.get(uuid).get()
                            .names.stream()
                            .map(Name::getName).collect(Collectors.toSet()));
            
            
            
            //version must update
            assertEquals("2",
                    substanceEntityService.get(uuid).get()
                           .version);


//            js =api.fetchSubstanceJsonByUuid(s.getUuid().toString());
//
//            SubstanceBuilder.from(js)
//                    .addName("thirdName")
////                            .setVersion(2)
//                    .buildJsonAnd(js2-> SubstanceJsonUtil.ensurePass(api.updateSubstance(js2)));

            assertEquals(1, PersistedSubstanceProcessorTestDouble.timesNewCalled);
            assertEquals(1, PersistedSubstanceProcessorTestDouble.timesUpdatedCalled);

//            assertEquals(new HashSet<>(Arrays.asList("aName", "secondNme")),
//                    PersistedSubstanceProcessorTestDouble.lastUpdatedNew.names.stream()
//                            .map(Name::getName).collect(Collectors.toSet()));
//
//            assertEquals(new HashSet<>(Arrays.asList("aName")),
//                    PersistedSubstanceProcessorTestDouble.lastUpdatedOld.names.stream()
//                            .map(Name::getName).collect(Collectors.toSet()));




    }
    @TestComponent
    public static class PersistedSubstanceProcessorTestDouble implements EntityProcessor<Substance> {

        @Autowired
        private EditRepository editRepository;


        @Override
        public void postUpdate(Substance obj) throws FailProcessingException {
            timesUpdatedCalled++;
            System.out.println("post UPDATE substance id " + obj);
        }

        @Override
        public Class<Substance> getEntityClass() {
            return Substance.class;
        }

        @Override
        public void postPersist(Substance obj) throws FailProcessingException {
            System.out.println("post persist substance id " + obj);
            timesNewCalled++;

        }



//        public void fireNewOrUpdate(Substance obj) {
//            int version;
//            try {
//                version = Integer.parseInt(obj.version);
//            }catch(RuntimeException e){
//                e.printStackTrace();
//                throw e;
//            }
//
//            if(version ==1){
//                handleNewSubstance(obj);
//            }else{
//                String oldVersion = Integer.toString(version -1);
//
//                Optional<Edit> oldEdit = editRepository.findByRefidAndVersion(obj.uuid.toString(), oldVersion);
////
////                if(oldEdit.isPresent()){
////                    try{
////                        Edit e = oldEdit.get();
//////                        Substance oldSubstance = (Substance) SubstanceBuilder.from(e.getOldValueReference().rawJson()).build();
//////
//////
//////                        handleUpdatedSubstance(oldSubstance, obj);
////                    }catch(Exception ex){
////                        throw new IllegalArgumentException(ex);
////                    }
////                }else{
//////                System.out.println("no edit?");
////                    //no old edit and not version 1 ?
////                    //assume new substance ?
////                    handleNewSubstance(obj);
////                }
//            }
//        }

        public static int timesNewCalled=0;
        public static int timesUpdatedCalled=0;
        public static Substance lastNewSubstance=null;
        public static Substance lastUpdatedOld, lastUpdatedNew;

        public PersistedSubstanceProcessorTestDouble(){
            reset();

        }

        public void reset() {
            timesNewCalled=0;
            timesUpdatedCalled=0;
            lastNewSubstance=null;
            lastUpdatedOld=null;
            lastUpdatedNew=null;
        }


        protected void handleNewSubstance(Substance substance) {
            timesNewCalled++;
            lastNewSubstance = substance;
        }


        protected void handleUpdatedSubstance(Substance oldSubstance, Substance newSubstance) {
            timesUpdatedCalled++;
//            lastUpdatedOld=oldSubstance;
//            lastUpdatedNew = newSubstance;
        }
    }
}

