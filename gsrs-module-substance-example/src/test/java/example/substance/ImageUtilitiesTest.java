package example.substance;

import gsrs.module.substance.utils.ImageUtilities;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.EmbeddedKeywordList;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

public class ImageUtilitiesTest {

    @Test
    public void testSubstanceWithoutImage(){
        SubstanceBuilder builder = new SubstanceBuilder();
        Name plainName = new Name();
        plainName.name="Plain Substance";
        plainName.displayName=true;

        Reference reference = new Reference();
        reference.publicDomain= true;
        reference.docType="book";
        reference.citation="Descriptions of stuff, page 203";
        reference.addTag("PUBLIC_DOMAIN_RELEASE");
        plainName.addReference(reference);
        builder.addName(plainName);
        builder.addReference(reference);
        Substance substance = builder.build();
        Optional<byte[]> bytes= ImageUtilities.getSubstanceImage(substance);
        Assertions.assertFalse(bytes.isPresent());
    }

    @Test
    public void testSubstanceWithImage() throws IOException {
        SubstanceBuilder builder = new SubstanceBuilder();
        Name plainName = new Name();
        plainName.name="Plain Substance";
        plainName.displayName=true;

        Reference reference = new Reference();
        reference.publicDomain= true;
        reference.docType="book";
        reference.citation="Descriptions of stuff, page 203";
        reference.addTag("PUBLIC_DOMAIN_RELEASE");
        reference.addTag(ImageUtilities.SUBSTANCE_IMAGE_TAG);
        reference.uploadedFile="https://upload.wikimedia.org/wikipedia/commons/1/1d/Feldspar-Group-291254.jpg";
        plainName.addReference(reference);
        builder.addName(plainName);
        builder.addReference(reference);
        Substance substance = builder.build();
        Optional<byte[]> bytes= ImageUtilities.getSubstanceImage(substance);
        Assertions.assertTrue(bytes.isPresent());
    }

}
