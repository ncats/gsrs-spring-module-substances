package example.substance;

import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.NameOrg;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("fullstack")
public class UpdateNameTest  extends AbstractSubstanceJpaEntityTest {


    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    public void addNameOrg(){
        Substance created = assertCreated(new SubstanceBuilder()
                .addName("Concept Name")
                .buildJson());
        UUID uuid = created.getUuid();

        Substance old= substanceEntityService.get(uuid).get();

        old.toBuilder()
                .andThen(s-> {
                    NameOrg org = new NameOrg();
                    org.setUuid(UUID.randomUUID());
                    org.nameOrg = "MyName org";
                    s.getAllNames().get(0).nameOrgs.add(org);
                })
                .buildJsonAnd(this::assertUpdated);
        Substance updated = substanceEntityService.get(uuid).get();
        NameOrg actualNameOrg = updated.getAllNames().get(0).nameOrgs.get(0);

        assertEquals("MyName org", actualNameOrg.nameOrg);
        UUID nameOrgId = actualNameOrg.getUuid();
        assertNotNull(nameOrgId);
    }

    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    public void updateChemicalStructureWithExistingNameOrg(){
        UUID nameOrgUuid = UUID.randomUUID();

        Substance created = assertCreated(new SubstanceBuilder()
                .asChemical()
                .setStructureWithDefaultReference("CCO")
                .addName("Chemical Name", n -> {
                    NameOrg org = new NameOrg();
                    org.setUuid(nameOrgUuid);
                    org.nameOrg = "MyName org";
                    n.nameOrgs.add(org);
                })
                .buildJson());

        ChemicalSubstanceBuilder updateBuilder = SubstanceBuilder.from(created.toFullJsonNode());
        Substance updated = assertUpdated(updateBuilder
                .setStructureWithDefaultReference("CCCO")
                .buildJson());

        List<NameOrg> actualNameOrgs = updated.getAllNames().get(0).nameOrgs;

        assertThat(actualNameOrgs.stream().map(no -> no.nameOrg).collect(Collectors.toList()),
                contains("MyName org"));
        assertEquals(nameOrgUuid, actualNameOrgs.get(0).getUuid());
    }




    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    public void addNameOrg_MitchAlt3x(){
        // Temporary modified copy
        // Mitch discovered that the test can work like this.
        // Perhaps provides a clue on a broader fix of the problems
        // with UUID generation in the database

        Substance created = assertCreated(new SubstanceBuilder()
                .addName("Concept Name")
                .buildJson());
        UUID uuid = created.getUuid();

        Substance old= substanceEntityService.get(uuid).get();

        old.toBuilder()
                .andThen(s-> {
                    NameOrg org = new NameOrg();
                    org.setUuid(UUID.randomUUID());
                    org.nameOrg = "MyName org";
                    s.getAllNames().get(0).nameOrgs.add(org);
                    s.version = "2";
                })
                .buildJsonAnd(this::assertUpdated);
        Substance updated = substanceEntityService.get(uuid).get();
        NameOrg actualNameOrg = updated.getAllNames().get(0).nameOrgs.get(0);

        assertEquals("MyName org", actualNameOrg.nameOrg);
        UUID nameOrgId = actualNameOrg.getUuid();
        assertNotNull(nameOrgId);
    }



    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    public void addNameOrgToSecondName(){
        Substance created = assertCreated(new SubstanceBuilder()
                .addName("Concept Name")
                .buildJson());
        UUID uuid = created.getUuid();

        Substance old= substanceEntityService.get(uuid).get();

        old.toBuilder()
                .addName("name2", n-> {
                    NameOrg org = new NameOrg();
                    org.nameOrg = "MyName org";
                    n.nameOrgs.add(org);
                })
                .buildJsonAnd(this::assertUpdated);
        Substance updated = substanceEntityService.get(uuid).get();
        List<Name> allNames = updated.getAllNames();
        assertThat(allNames.get(0).nameOrgs, is(empty()));

        NameOrg actualNameOrg = allNames.get(1).nameOrgs.get(0);

        assertEquals("MyName org", actualNameOrg.nameOrg);
        UUID nameOrgId = actualNameOrg.getUuid();
        assertNotNull(nameOrgId);
    }

    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    public void updateNameOrg(){
        Substance created = assertCreated(new SubstanceBuilder()
                .addName("Concept Name",n->{
                    NameOrg org = new NameOrg();
                    org.nameOrg = "MyName org";
                    org.setUuid(UUID.randomUUID());
                    n.nameOrgs.add(org);
                })
                .buildJson());
        UUID uuid = created.getUuid();
        Substance old= substanceEntityService.get(uuid).get();

        old.toBuilder()
                .andThen(s-> {
                    NameOrg org = new NameOrg();
                    org.nameOrg = "MyName org2";
                    org.setUuid(UUID.randomUUID());
                    s.getAllNames().get(0).nameOrgs.add(org);
                    s.changeReason ="changed name orgs";
                })
                .buildJsonAnd(this::assertUpdated);
        Substance updated = substanceEntityService.get(uuid).get();
        List<NameOrg> actualNameOrgs = updated.getAllNames().get(0).nameOrgs;

        assertThat(updated.getAllNames().get(0).nameOrgs.stream().map(n-> n.nameOrg).collect(Collectors.toList()),
                containsInAnyOrder("MyName org", "MyName org2"));

    }

    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    public void updateNameOrg_MitchAlt3x(){
        // Temporary modified copy
        // Mitch discovered that the test can work like this.
        // Perhaps provides a clue on a broader fix of the problems
        // with UUID generation in the database

        Substance created = assertCreated(new SubstanceBuilder()
                .addName("Concept Name",n->{
                    NameOrg org = new NameOrg();
                    org.nameOrg = "MyName org";
                    n.nameOrgs.add(org);
                })
                .buildJson());
        UUID uuid = created.getUuid();
        Substance old= substanceEntityService.get(uuid).get();

        old.toBuilder()
                .andThen(s-> {
                    NameOrg org = new NameOrg();
                    org.nameOrg = "MyName org2";
                    org.setUuid(UUID.randomUUID());
                    s.getAllNames().get(0).nameOrgs.add(org);
                    String oldVersion = s.version;
                    int oldVersionNumber = Integer.parseInt(oldVersion);
                    s.version = Integer.toString(oldVersionNumber+1);
                })
                .buildJsonAnd(this::assertUpdated);
        Substance updated = substanceEntityService.get(uuid).get();
        List<NameOrg> actualNameOrgs = updated.getAllNames().get(0).nameOrgs;

        assertThat(updated.getAllNames().get(0).nameOrgs.stream().map(n-> n.nameOrg).collect(Collectors.toList()),
                containsInAnyOrder("MyName org", "MyName org2"));

    }


    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    public void update2ndNameOrg(){
        Substance created = assertCreated(new SubstanceBuilder()
                .addName("Concept Name",n->{
                    NameOrg org = new NameOrg();
                    org.nameOrg = "MyName org";
                    n.nameOrgs.add(org);
                })
                .addName("name 2")
                .buildJson());
        UUID uuid = created.getUuid();
        Substance old= substanceEntityService.get(uuid).get();

        SubstanceBuilder.from(old.toFullJsonNode())
                .andThen(s -> {
                    NameOrg org = new NameOrg();
                    org.nameOrg = "MyName org2";
                    org.setUuid(UUID.randomUUID());
                    s.getAllNames().get(1).nameOrgs.add(org);
                })
                .buildJsonAnd(this::assertUpdated);
        Substance updated = substanceEntityService.get(uuid).get();


        assertThat(updated.getAllNames().get(0).nameOrgs.stream().map(n-> n.nameOrg).collect(Collectors.toList()),
                containsInAnyOrder("MyName org"));
        assertThat(updated.getAllNames().get(1).nameOrgs.stream().map(n-> n.nameOrg).collect(Collectors.toList()),
                containsInAnyOrder("MyName org2"));

    }


    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    public void update2ndNameOrg_MitchAlt3x(){
        // Temporary modified copy
        // Mitch discovered that the test can work like this.
        // Perhaps provides a clue on a broader fix of the problems
        // with UUID generation in the database

        Substance created = assertCreated(new SubstanceBuilder()
                .addName("Concept Name",n->{
                    NameOrg org = new NameOrg();
                    org.nameOrg = "MyName org";
                    org.setUuid(UUID.randomUUID());
                    n.nameOrgs.add(org);
                })
                .addName("name 2")
                .buildJson());
        UUID uuid = created.getUuid();
        Substance old= substanceEntityService.get(uuid).get();

        old.toBuilder()
                .andThen(s-> {
                    NameOrg org = new NameOrg();
                    org.nameOrg = "MyName org2";
                    org.setUuid(UUID.randomUUID());
                    s.getAllNames().get(1).nameOrgs.add(org);
                    s.version = "2";
                })
                .buildJsonAnd(this::assertUpdated);
        Substance updated = substanceEntityService.get(uuid).get();


        assertThat(updated.getAllNames().get(0).nameOrgs.stream().map(n-> n.nameOrg).collect(Collectors.toList()),
                containsInAnyOrder("MyName org"));
        assertThat(updated.getAllNames().get(1).nameOrgs.stream().map(n-> n.nameOrg).collect(Collectors.toList()),
                containsInAnyOrder("MyName org2"));

    }

}
