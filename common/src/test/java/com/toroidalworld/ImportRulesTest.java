package com.toroidalworld;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class ImportRulesTest {
    private static final String REPO_ROOT_PROPERTY = "toroidal.repoRoot";
    private static final List<String> SOURCE_ROOTS =
            List.of("common/src/main/java", "neoforge/src/main/java", "fabric/src/main/java");
    private static final String PACKAGE_PATH = "com/toroidalworld";
    private static final String PACKAGE_PREFIX = "com.toroidalworld.";
    private static final String IMPORT_KEYWORD = "import ";
    private static final String STATIC_KEYWORD = "static ";
    private static final String JAVA_SUFFIX = ".java";
    private static final String MIXIN_SEGMENT = "mixin";
    private static final String ROOT_GROUP = "";

    private static final Set<String> ENGINE_READERS =
            Set.of("engine", "mixin", "accessors", "compat", "platform", "client.engine");
    private static final Set<String> LOADER_ENTRIES =
            Set.of("WorldLoop", "WorldLoopNetwork", "ToroidalWorldNeoForge", "ToroidalWorldFabric",
                    "ToroidalWorldFabricClient", "VanillaBootstrapListener");
    private static final Set<String> EXCEPTIONS = Set.of(
            "engine.noise.ClimateScaleCompression -> shape.torus.ClimateScale",
            "engine.noise.ClimateScaleCompression -> shape.torus.CompactBiomes",
            "engine.noise.CoastFieldLift -> shape.torus.GuaranteedLand",
            "client.shape.torus.ClimateFactorPreview -> engine.noise.ClimateFields",
            "client.shape.torus.ClimateFactorPreview -> engine.noise.ClimateScaleCompression",
            "shape.WorldShapes -> engine.gen.ShapedDimensions",
            "shape.torus.TorusDimensions -> engine.gen.ShapedDimensions",
            "shape.cylinder.CylinderDimensions -> engine.gen.ShapedDimensions",
            "core.WorldLoopAttachments -> engine.seam.ClientPosition",
            "core.WorldLoopAttachments -> engine.seam.SeamTravel");

    @Test
    void everyImportStaysInsideItsGroup() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path source : sourceFiles()) {
            String type = typeOf(source);
            for (String imported : toroidalImports(source)) {
                String edge = type + " -> " + imported;
                String broken = brokenRule(type, imported);
                if (broken != null && !EXCEPTIONS.contains(edge)) {
                    violations.add(edge + " (" + broken + ")");
                }
            }
        }

        assertTrue(violations.isEmpty(), "imports crossing a group boundary: " + violations.stream().sorted().toList());
    }

    private static String brokenRule(String reader, String imported) {
        String readerGroup = groupOf(packageOf(reader));
        String importedGroup = groupOf(packageOf(imported));
        if (importedGroup.equals("engine")
                && !ENGINE_READERS.contains(readerGroup)
                && !LOADER_ENTRIES.contains(simpleNameOf(reader))) {
            return "engine is read by mixins, accessors, compat, platform, client/engine and the loader entries alone";
        }
        if (readerGroup.equals("engine") && (importedGroup.equals("shape") || importedGroup.equals("client"))) {
            return "engine never reads a shape or the client";
        }
        if (readerGroup.equals("core") && !importedGroup.equals("core") && !isVocabulary(importedGroup)) {
            return "core is the vocabulary: it reads no work";
        }
        return null;
    }

    private static boolean isVocabulary(String group) {
        return group.equals("accessors") || group.equals("api") || group.equals("platform") || group.equals(ROOT_GROUP);
    }

    private static String groupOf(String packageName) {
        List<String> segments = List.of(packageName.split("\\."));
        if (segments.contains(MIXIN_SEGMENT)) {
            return MIXIN_SEGMENT;
        }
        if (packageName.startsWith("client.engine")) {
            return "client.engine";
        }
        return segments.isEmpty() ? ROOT_GROUP : segments.get(0);
    }

    private static String packageOf(String type) {
        int lastDot = type.lastIndexOf('.');
        return lastDot < 0 ? ROOT_GROUP : type.substring(0, lastDot);
    }

    private static String simpleNameOf(String type) {
        return type.substring(type.lastIndexOf('.') + 1);
    }

    private static List<String> toroidalImports(Path source) throws IOException {
        List<String> imported = new ArrayList<>();
        for (String line : Files.readAllLines(source, StandardCharsets.UTF_8)) {
            String statement = line.strip();
            if (!statement.startsWith(IMPORT_KEYWORD)) {
                continue;
            }

            statement = statement.substring(IMPORT_KEYWORD.length()).strip();
            if (statement.startsWith(STATIC_KEYWORD)) {
                statement = statement.substring(STATIC_KEYWORD.length()).strip();
            }
            if (!statement.startsWith(PACKAGE_PREFIX)) {
                continue;
            }

            imported.add(topLevelType(statement.substring(PACKAGE_PREFIX.length()).replace(";", "").strip()));
        }
        return imported;
    }

    private static String topLevelType(String reference) {
        List<String> segments = List.of(reference.split("\\."));
        List<String> upTo = new ArrayList<>();
        for (String segment : segments) {
            upTo.add(segment);
            if (Character.isUpperCase(segment.charAt(0))) {
                break;
            }
        }
        return String.join(".", upTo);
    }

    private static String typeOf(Path source) {
        String path = source.toString().replace('\\', '/');
        String relative = path.substring(path.lastIndexOf(PACKAGE_PATH) + PACKAGE_PATH.length() + 1);
        return relative.substring(0, relative.length() - JAVA_SUFFIX.length()).replace('/', '.');
    }

    private static List<Path> sourceFiles() throws IOException {
        String repositoryRoot = System.getProperty(REPO_ROOT_PROPERTY);
        assertNotNull(repositoryRoot, "the " + REPO_ROOT_PROPERTY + " system property names no repository root");

        List<Path> sources = new ArrayList<>();
        for (String sourceRoot : SOURCE_ROOTS) {
            Path root = Path.of(repositoryRoot, sourceRoot, PACKAGE_PATH);
            assertTrue(Files.isDirectory(root), "missing source root " + root);
            try (Stream<Path> files = Files.walk(root)) {
                files.filter(file -> file.getFileName().toString().endsWith(JAVA_SUFFIX)).forEach(sources::add);
            }
        }
        return sources;
    }
}
