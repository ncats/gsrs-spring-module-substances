package example.substance;


import gsrs.services.PrincipalService;
import gsrs.springUtils.AutowireHelper;
import ix.core.models.Principal;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;


import java.io.File;
import java.io.InputStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by katzelda on 9/6/16.
 */
@RunWith(SpringRunner.class)
@Import(AutowireHelper.class)
public class SubstanceBuilderTest {

    @MockBean
    PrincipalService principalRepository;



    @Before
    public void setup(){
        Principal fdaSrs = new Principal("FDA_SRS", null);
        Mockito.when(principalRepository.registerIfAbsent("FDA_SRS")).thenReturn(fdaSrs);
    }

    @Test
    public void setName(){
        Substance substance = new SubstanceBuilder()
                                    .addName("foo")
                                    .build();

        assertEquals("foo", substance.getName());
    }

    @Test
    public void ChemicalSubstanceFromJsonInputStream() throws Exception{
        try(InputStream in = getClass().getResourceAsStream("/testJSON/pass/2moities.json")){
            assertNotNull(in);
            ChemicalSubstanceBuilder builder = SubstanceBuilder.from(in);

            assert2MoietiesBuiltCorrectly(builder);
        }
    }

    @Test
    public void ChemicalSubstanceFromJsonFile() throws Exception{
        ChemicalSubstanceBuilder builder = SubstanceBuilder.from(new File(getClass().getResource("/testJSON/pass/2moities.json").getFile()));

        assert2MoietiesBuiltCorrectly(builder);

    }

    private void assert2MoietiesBuiltCorrectly(ChemicalSubstanceBuilder builder) {
        ChemicalSubstance substance = builder.build();

        assertEquals("1db30542-0cc4-4098-9d89-8340926026e9", substance.getUuid().toString());
        assertEquals(2, substance.moieties.size());
    }

    @Test
    public void modifyChemicalSubstanceFromJson() throws Exception{
        ///home/katzelda/GIT/inxight3/modules/ginas/test/testJSON/pass/2moities.json
        String path = "test/testJSON/pass/2moities.json";

        try(InputStream in = getClass().getResourceAsStream("/testJSON/pass/2moities.json")){
            assertNotNull(in);
            ChemicalSubstanceBuilder builder = SubstanceBuilder.from(in);

            UUID uuid = UUID.randomUUID();
            builder.setUUID( uuid);
            ChemicalSubstance substance = builder.build();

            assertEquals(uuid, substance.getUuid());
            assertEquals(2, substance.moieties.size());
        }
    }
}
