package ix.ginas.models.utils;

import com.sleepycat.je.log.LogEntryType;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.Properties;

@Slf4j
public class MavenUtils {

    public static String getVersion(Class<?> clazz, String groupId, String artifactId) {
        log.trace("starting getVersion with groupid {} and artifactId {}", groupId, artifactId);
        String version = null;
        try {
            String path = "/META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";
            try (InputStream is = clazz.getResourceAsStream(path)) {
                if (is != null) {
                    Properties props = new Properties();
                    props.load(is);
                    version = props.getProperty("version");
                    for(String propName : props.stringPropertyNames()) {
                        log.trace("property {} = {}", propName, props.getProperty(propName));
                    }
                }
            }
        } catch (Exception e) {
            // ignore
            log.error("error parsing maven info", e);
        }
        if (version == null) {
            Package p = clazz.getPackage();
            if (p != null) {
                version = p.getImplementationVersion();
                if (version == null) {
                    version = p.getSpecificationVersion();
                }
            }
        }
        return version != null ? version : "unknown";
    }
}
