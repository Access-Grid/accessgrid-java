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
 * Locks the AccessGridClient.VERSION constant and the README install snippets to
 * the pom.xml project version. Drift has shipped before (1.3.0 constant under a
 * 1.4.0 pom; a stale 1.3.0 README install line under a 1.4.2 pom), so this test
 * fails the build if any of them disagree.
 */
public class VersionConsistencyTest {

    private static String pomVersion() throws IOException {
        String pom = Files.readString(Paths.get("pom.xml"));
        Matcher m = Pattern.compile(
            "<artifactId>access-grid-sdk</artifactId>\\s*<version>([^<]+)</version>"
        ).matcher(pom);
        assertTrue(m.find(), "Could not find <version> for access-grid-sdk in pom.xml");
        return m.group(1);
    }

    @Test
    public void versionConstantMatchesPomXml() throws IOException, NoSuchFieldException, IllegalAccessException {
        Field versionField = AccessGridClient.class.getDeclaredField("VERSION");
        versionField.setAccessible(true);
        String constantVersion = (String) versionField.get(null);

        assertEquals(
            pomVersion(),
            constantVersion,
            "AccessGridClient.VERSION must match pom.xml <version>. Update both when bumping."
        );
    }

    @Test
    public void readmeInstallSnippetsMatchPomXml() throws IOException {
        String pomVersion = pomVersion();
        String readme = Files.readString(Paths.get("README.md"));

        Matcher maven = Pattern.compile(
            "<artifactId>access-grid-sdk</artifactId>\\s*<version>([^<]+)</version>"
        ).matcher(readme);
        assertTrue(maven.find(), "Could not find the Maven install snippet in README.md");
        assertEquals(
            pomVersion,
            maven.group(1),
            "README Maven <version> must match pom.xml <version>. Update the README when bumping."
        );

        Matcher gradle = Pattern.compile(
            "com\\.accessgrid:access-grid-sdk:([^'\"]+)"
        ).matcher(readme);
        assertTrue(gradle.find(), "Could not find the Gradle install snippet in README.md");
        assertEquals(
            pomVersion,
            gradle.group(1),
            "README Gradle coordinate must match pom.xml <version>. Update the README when bumping."
        );
    }
}
