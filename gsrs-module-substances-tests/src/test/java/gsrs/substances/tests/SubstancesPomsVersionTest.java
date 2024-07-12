package gsrs.substances.tests;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import gsrs.startertests.pomutilities.PomUtilities;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SubstancesPomsVersionTest {
    // Check GSRS versions in all pom.xml, and select extraJars, installExtraJars.sh commands
    // against the values in pom-version.properties. Helpful when making a version change
    // for the whole GSRS project.
    // Test effectively skipped unless CLI param -DdoPomCheck=true
    // Run from command line:
    // mvn test -Dtest=gsrs.substances.tests.SubstancesPomsVersionTest -DdoPomCheck=true -pl gsrs-module-substances-tests

    // Set to true when testing in IDE
    boolean turnOffPomParameterCheck = false;

    String shortVersion;
    String longVersion;
    String rootDir;
    String propertiesFile;
    String installExtraJarsScriptText;
    boolean doPomCheck = false;

    @BeforeEach
    public void setup() {
        doPomCheck=Boolean.parseBoolean(System.getProperty("doPomCheck"));
        if(!doPomCheck && !turnOffPomParameterCheck) { return; }
        String scriptFile = "installExtraJars.sh";
        propertiesFile = "pom-version.properties";
        rootDir = "..";

        try {
            Properties properties = PomUtilities.readPomVersionProperties(rootDir + "/" + propertiesFile);
            shortVersion = properties.getProperty("project.shortVersion");
            longVersion = properties.getProperty("project.longVersion");
            assertNotNull(shortVersion);
            System.out.println("shortVersion: " + shortVersion);
            System.out.println("longVersion: " + longVersion);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            installExtraJarsScriptText = PomUtilities.readTextFile(rootDir + "/" + scriptFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testPomCheck() {
        if(!doPomCheck && !turnOffPomParameterCheck) {
            System.out.println("Effectively skipping testPomCheck because -DdoPomCheck is not true.");
            return;
        }
        Model rootModel;
        try {
            rootModel = PomUtilities.readPomToModel(rootDir + "/pom.xml");
            Properties properties = rootModel.getProperties();
            System.out.println("> checking root");
            assertEquals( longVersion, rootModel.getVersion(), "version");
            assertEquals(longVersion, properties.getProperty("gsrs.version"), "gsrs.version:");
            List<String> modules = rootModel.getModules();
            for (String module : modules) {
                System.out.println("> checking "+ module);
                Model moduleModel;
                try {
                    moduleModel = PomUtilities.readPomToModel(rootDir + "/" + module + "/pom.xml");
                    String checkVersion = moduleModel.getParent().getVersion();
                    assertEquals (longVersion, checkVersion, "parent/version");

                    if (module.equals("gsrs-fda-substance-extension")) {
                        List<Dependency> dependencies = moduleModel.getDependencies();
                        boolean applicationsApiChecked = false;
                        boolean clinicalTrialsApiChecked = false;
                        boolean productsApiChecked = false;

                        for (Dependency dependency : dependencies) {
                            if (dependency.getGroupId().equals("gov.nih.ncats") && dependency.getArtifactId().equals("applications-api")) {
                                checkDependencyExtraJarExistsAndFindPathInScript(dependency);
                                applicationsApiChecked = true;
                            }
                            if (dependency.getGroupId().equals("gov.nih.ncats") && dependency.getArtifactId().equals("clinical-trials-api")) {
                                checkDependencyExtraJarExistsAndFindPathInScript(dependency);
                                clinicalTrialsApiChecked = true;
                            }
                            if (dependency.getGroupId().equals("gov.nih.ncats") && dependency.getArtifactId().equals("products-api")) {
                                checkDependencyExtraJarExistsAndFindPathInScript(dependency);
                                productsApiChecked = true;
                            }
                        }
                        assertTrue(applicationsApiChecked);
                        assertTrue(clinicalTrialsApiChecked);
                        assertTrue(productsApiChecked);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void checkDependencyExtraJarExistsAndFindPathInScript(Dependency dependency) {
        String jarPath = "extraJars/" + PomUtilities.makeJarFilename(dependency);
        File file = new File(rootDir + "/" + jarPath);
        assertTrue(file.exists());
        assertTrue(installExtraJarsScriptText.contains(jarPath));
    }
}
