package example.substance;

import com.fasterxml.jackson.databind.JsonNode;
import ix.core.util.EntityUtils;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.NameOrg;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class UpdateNameTest  extends AbstractSubstanceJpaEntityTest {


    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    public void addNameOrg(){
        UUID uuid = UUID.randomUUID();

        new SubstanceBuilder()
                .addName("Concept Name")
                .setUUID(uuid)
                .buildJsonAnd(this::assertCreated);

        Optional<Substance> old= substanceEntityService.get(uuid);

        old.get().toBuilder()
                .andThen(s-> {
                    NameOrg org = new NameOrg();
                    org.nameOrg = "MyName org";
                    s.getAllNames().get(0).nameOrgs.add(org);
                })
                .buildJsonAnd(this::assertUpdated);
        Optional<Substance> updated = substanceEntityService.get(uuid);
        NameOrg actualNameOrg = updated.get().getAllNames().get(0).nameOrgs.get(0);

        assertEquals("MyName org", actualNameOrg.nameOrg);
        UUID nameOrgId = actualNameOrg.getUuid();
        assertNotNull(nameOrgId);
    }

    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    public void addNameOrgToSecondName(){
        UUID uuid = UUID.randomUUID();

        new SubstanceBuilder()
                .addName("Concept Name")
                .setUUID(uuid)
                .buildJsonAnd(this::assertCreated);

        Optional<Substance> old= substanceEntityService.get(uuid);

        old.get().toBuilder()
                .addName("name2", n-> {
                    NameOrg org = new NameOrg();
                    org.nameOrg = "MyName org";
                    n.nameOrgs.add(org);
                })
                .buildJsonAnd(this::assertUpdated);
        Optional<Substance> updated = substanceEntityService.get(uuid);
        List<Name> allNames = updated.get().getAllNames();
        assertThat(allNames.get(0).nameOrgs, is(empty()));

        NameOrg actualNameOrg = allNames.get(1).nameOrgs.get(0);

        assertEquals("MyName org", actualNameOrg.nameOrg);
        UUID nameOrgId = actualNameOrg.getUuid();
        assertNotNull(nameOrgId);
    }

    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    public void updateNameOrg(){
        UUID uuid = UUID.randomUUID();

        new SubstanceBuilder()
                .addName("Concept Name",n->{
                    NameOrg org = new NameOrg();
                    org.nameOrg = "MyName org";
                    n.nameOrgs.add(org);
                })
                .setUUID(uuid)
                .buildJsonAnd(this::assertCreated);
        Optional<Substance> old= substanceEntityService.get(uuid);

        old.get().toBuilder()
                .andThen(s-> {
                    NameOrg org = new NameOrg();
                    org.nameOrg = "MyName org2";
                    s.getAllNames().get(0).nameOrgs.add(org);
                })
                .buildJsonAnd(this::assertUpdated);
        Optional<Substance> updated = substanceEntityService.get(uuid);
        List<NameOrg> actualNameOrgs = updated.get().getAllNames().get(0).nameOrgs;

        assertThat(updated.get().getAllNames().get(0).nameOrgs.stream().map(n-> n.nameOrg).collect(Collectors.toList()),
                containsInAnyOrder("MyName org", "MyName org2"));

    }

    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    public void update2ndNameOrg(){
        UUID uuid = UUID.randomUUID();

        new SubstanceBuilder()
                .addName("Concept Name",n->{
                    NameOrg org = new NameOrg();
                    org.nameOrg = "MyName org";
                    n.nameOrgs.add(org);
                })
                .addName("name 2")
                .setUUID(uuid)
                .buildJsonAnd(this::assertCreated);
        Optional<Substance> old= substanceEntityService.get(uuid);

        old.get().toBuilder()
                .andThen(s-> {
                    NameOrg org = new NameOrg();
                    org.nameOrg = "MyName org2";
                    s.getAllNames().get(1).nameOrgs.add(org);
                })
                .buildJsonAnd(this::assertUpdated);
        Optional<Substance> updated = substanceEntityService.get(uuid);


        assertThat(updated.get().getAllNames().get(0).nameOrgs.stream().map(n-> n.nameOrg).collect(Collectors.toList()),
                containsInAnyOrder("MyName org"));
        assertThat(updated.get().getAllNames().get(1).nameOrgs.stream().map(n-> n.nameOrg).collect(Collectors.toList()),
                containsInAnyOrder("MyName org2"));

    }
}
