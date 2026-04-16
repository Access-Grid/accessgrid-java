package com.organization.accessgrid;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates that .tool-versions, CI workflow, and Maven wrapper
 * all target consistent versions.
 */
public class VersionConsistencyTest {

    // ══════════════════════════════════════════════════════════════════════════
    // TARGET VERSIONS - Update these when upgrading
    // ══════════════════════════════════════════════════════════════════════════
    private static final String TARGET_JAVA = "temurin-11.0.30+7";
    private static final String TARGET_MAVEN = "3.9.9";

    private static final Path ROOT = Paths.get(System.getProperty("user.dir"));

    private String readFile(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }

    private String majorVersion(String javaVersion) {
        // "temurin-11.0.30+7" → "11"
        Matcher m = Pattern.compile("(\\d+)").matcher(javaVersion);
        if (m.find()) {
            return m.group(1);
        }
        return javaVersion;
    }

    private String toolVersionsEntry(String content, String tool) {
        for (String line : content.split("\n")) {
            String[] parts = line.trim().split("\\s+", 2);
            if (parts.length == 2 && parts[0].equals(tool)) {
                return parts[1];
            }
        }
        return null;
    }

    @Test
    public void toolVersionsJavaMatchesTarget() throws IOException {
        String content = readFile(".tool-versions");
        String javaVersion = toolVersionsEntry(content, "java");

        assertNotNull(javaVersion, ".tool-versions should contain a java entry");
        assertEquals(TARGET_JAVA, javaVersion,
            ".tool-versions java should match TARGET_JAVA");
    }

    @Test
    public void toolVersionsMavenMatchesTarget() throws IOException {
        String content = readFile(".tool-versions");
        String mavenVersion = toolVersionsEntry(content, "maven");

        assertNotNull(mavenVersion, ".tool-versions should contain a maven entry");
        assertEquals(TARGET_MAVEN, mavenVersion,
            ".tool-versions maven should match TARGET_MAVEN");
    }

    @Test
    public void ciMatrixIncludesTargetJava() throws IOException {
        String content = readFile(".github/workflows/ci.yml");

        // Extract java: ['11', '17', '21'] matrix
        Matcher m = Pattern.compile("java:\\s*\\[([^\\]]+)\\]").matcher(content);
        assertTrue(m.find(), "CI workflow should contain a java version matrix");

        String raw = m.group(1);
        List<String> versions = new ArrayList<>();
        for (String v : raw.split(",")) {
            versions.add(v.trim().replaceAll("['\"]", ""));
        }

        String targetMajor = majorVersion(TARGET_JAVA);
        assertTrue(versions.contains(targetMajor),
            "CI matrix " + versions + " should include target major version " + targetMajor);
    }

    @Test
    public void mavenWrapperMatchesTarget() throws IOException {
        String content = readFile(".mvn/wrapper/maven-wrapper.properties");

        // Extract version from distributionUrl=...apache-maven-3.9.9-bin.zip
        Matcher m = Pattern.compile("apache-maven-([\\d.]+)-bin\\.zip").matcher(content);
        assertTrue(m.find(), "maven-wrapper.properties should contain a distributionUrl");

        String wrapperMaven = m.group(1);
        assertEquals(TARGET_MAVEN, wrapperMaven,
            "Maven wrapper version should match TARGET_MAVEN");
    }
}
