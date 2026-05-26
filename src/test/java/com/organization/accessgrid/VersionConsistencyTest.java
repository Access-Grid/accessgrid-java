package com.organization.accessgrid;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Locks the AccessGridClient.VERSION constant to the pom.xml project version.
 * Drift between the two has shipped before (1.3.0 constant under a 1.4.0 pom),
 * so this test fails the build if they disagree.
 */
public class VersionConsistencyTest {

    @Test
    public void versionConstantMatchesPomXml() throws IOException, NoSuchFieldException, IllegalAccessException {
        String pom = Files.readString(Paths.get("pom.xml"));

        Matcher m = Pattern.compile(
            "<artifactId>access-grid-sdk</artifactId>\\s*<version>([^<]+)</version>"
        ).matcher(pom);

        assertTrue(m.find(), "Could not find <version> for access-grid-sdk in pom.xml");
        String pomVersion = m.group(1);

        Field versionField = AccessGridClient.class.getDeclaredField("VERSION");
        versionField.setAccessible(true);
        String constantVersion = (String) versionField.get(null);

        assertEquals(
            pomVersion,
            constantVersion,
            "AccessGridClient.VERSION must match pom.xml <version>. Update both when bumping."
        );
    }
}
