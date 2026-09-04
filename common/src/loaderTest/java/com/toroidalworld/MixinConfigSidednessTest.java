package com.toroidalworld;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Timeout(60)
class MixinConfigSidednessTest {
    private static final String NEOFORGE_METADATA = "META-INF/neoforge.mods.toml";
    private static final String FABRIC_METADATA = "fabric.mod.json";
    private static final Pattern MOD_ID_ENTRY = Pattern.compile("modId\\s*=\\s*\"" + ToroidalWorld.MODID + "\"");
    private static final Pattern CONFIG_ENTRY = Pattern.compile("config\\s*=\\s*\"([^\"]+)\"");

    private static final String PACKAGE_KEY = "package";
    private static final String PLUGIN_KEY = "plugin";
    private static final String MIXINS_KEY = "mixins";
    private static final String SERVER_KEY = "server";
    private static final String CONFIG_KEY = "config";
    private static final String ID_KEY = "id";
    private static final List<String> BOTH_SIDES_KEYS = List.of(MIXINS_KEY, SERVER_KEY);

    private static final List<String> CLIENT_ONLY_ROOTS = List.of("net/minecraft/client/", "com/mojang/blaze3d/");
    private static final int CLASS_FILE_HEADER_BYTES = 8;
    private static final int CONSTANT_UTF8 = 1;
    private static final int CONSTANT_LONG = 5;
    private static final int CONSTANT_DOUBLE = 6;

    private static final String CLIENT_MIXIN_PACKAGE = "com.toroidalworld.client.shape.mixin";
    private static final String CLIENT_MIXIN_CLASS = "CreateWorldScreenMixin";

    private record Violation(String config, String owner, String clientType) {
        @Override
        public String toString() {
            return config + " -> " + owner + " reaches " + clientType;
        }
    }

    @Test
    void noClassPreparedOnADedicatedServerReachesAClientOnlyType() throws IOException {
        List<String> configs = declaredConfigs();
        assertFalse(configs.isEmpty(), "no mixin config declared by " + ToroidalWorld.MODID + " found on the classpath");

        List<Violation> violations = new ArrayList<>();
        for (String config : configs) {
            violations.addAll(lint(config, readConfig(config)));
        }
        if (!violations.isEmpty()) {
            fail(violations.size() + " class(es) prepared on a dedicated server reach a client-only type:\n"
                    + String.join("\n", violations.stream().map(Violation::toString).toList()));
        }
    }

    @Test
    void aClientMixinPlacedInABothSidesSectionIsReported() {
        JsonObject synthetic = new JsonObject();
        synthetic.addProperty(PACKAGE_KEY, CLIENT_MIXIN_PACKAGE);
        JsonArray mixins = new JsonArray();
        mixins.add(CLIENT_MIXIN_CLASS);
        synthetic.add(MIXINS_KEY, mixins);

        List<Violation> violations = lint("synthetic.mixins.json", synthetic);

        assertFalse(violations.isEmpty(),
                CLIENT_MIXIN_PACKAGE + "." + CLIENT_MIXIN_CLASS + " reaches no client-only type — the lint cannot fail");
    }

    private static List<Violation> lint(String config, JsonObject json) {
        List<Violation> violations = new ArrayList<>();
        for (String owner : ownersPreparedOnAServer(json)) {
            for (String clientType : clientTypesNamedBy(classBytes(owner))) {
                violations.add(new Violation(config, owner, clientType));
            }
        }
        return violations;
    }

    private static List<String> ownersPreparedOnAServer(JsonObject json) {
        List<String> owners = new ArrayList<>();
        JsonElement plugin = json.get(PLUGIN_KEY);
        if (plugin != null) {
            owners.add(plugin.getAsString());
        }
        for (String key : BOTH_SIDES_KEYS) {
            JsonArray section = json.getAsJsonArray(key);
            if (section == null) {
                continue;
            }
            String mixinPackage = json.get(PACKAGE_KEY).getAsString() + ".";
            for (JsonElement entry : section) {
                owners.add(mixinPackage + entry.getAsString());
            }
        }
        return owners;
    }

    private static byte[] classBytes(String className) {
        String resource = className.replace('.', '/') + ".class";
        try (InputStream stream = loader().getResourceAsStream(resource)) {
            assertNotNull(stream, "missing class resource " + resource);
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new AssertionError("cannot read " + resource, e);
        }
    }

    private static Set<String> clientTypesNamedBy(byte[] classFile) {
        Set<String> types = new LinkedHashSet<>();
        for (String constant : utf8Constants(classFile)) {
            for (String root : CLIENT_ONLY_ROOTS) {
                int at = constant.indexOf(root);
                while (at >= 0) {
                    types.add(typeAt(constant, at));
                    at = constant.indexOf(root, at + root.length());
                }
            }
        }
        return types;
    }

    private static String typeAt(String constant, int start) {
        int end = start;
        while (end < constant.length() && isTypeNameChar(constant.charAt(end))) {
            end++;
        }
        return constant.substring(start, end);
    }

    private static boolean isTypeNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '/' || c == '_' || c == '$';
    }

    private static List<String> utf8Constants(byte[] classFile) {
        List<String> constants = new ArrayList<>();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(classFile))) {
            in.skipBytes(CLASS_FILE_HEADER_BYTES);
            int poolSize = in.readUnsignedShort();
            for (int slot = 1; slot < poolSize; slot++) {
                int tag = in.readUnsignedByte();
                if (tag == CONSTANT_UTF8) {
                    constants.add(in.readUTF());
                    continue;
                }
                in.skipBytes(constantWidth(tag));
                if (tag == CONSTANT_LONG || tag == CONSTANT_DOUBLE) {
                    slot++;
                }
            }
        } catch (IOException e) {
            throw new AssertionError("malformed class file", e);
        }
        return constants;
    }

    private static int constantWidth(int tag) {
        return switch (tag) {
            case 7, 8, 16, 19, 20 -> 2;
            case 15 -> 3;
            case 3, 4, 9, 10, 11, 12, 17, 18 -> 4;
            case CONSTANT_LONG, CONSTANT_DOUBLE -> 8;
            default -> throw new AssertionError("unknown constant pool tag " + tag);
        };
    }

    private static List<String> declaredConfigs() throws IOException {
        Set<String> configs = new LinkedHashSet<>();
        for (URL metadata : resources(NEOFORGE_METADATA)) {
            String toml = read(metadata);
            if (!MOD_ID_ENTRY.matcher(toml).find()) {
                continue;
            }
            Matcher entries = CONFIG_ENTRY.matcher(toml);
            while (entries.find()) {
                configs.add(entries.group(1));
            }
        }
        for (URL metadata : resources(FABRIC_METADATA)) {
            JsonObject json = JsonParser.parseString(read(metadata)).getAsJsonObject();
            JsonElement id = json.get(ID_KEY);
            if (id == null || !ToroidalWorld.MODID.equals(id.getAsString())) {
                continue;
            }
            for (JsonElement entry : json.getAsJsonArray(MIXINS_KEY)) {
                configs.add(entry.isJsonObject() ? entry.getAsJsonObject().get(CONFIG_KEY).getAsString()
                        : entry.getAsString());
            }
        }
        return List.copyOf(configs);
    }

    private static JsonObject readConfig(String config) throws IOException {
        try (InputStream stream = loader().getResourceAsStream(config)) {
            assertNotNull(stream, "declared mixin config " + config + " is not on the classpath");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static List<URL> resources(String name) throws IOException {
        return Collections.list(loader().getResources(name));
    }

    private static String read(URL url) throws IOException {
        try (InputStream stream = url.openStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static ClassLoader loader() {
        return MixinConfigSidednessTest.class.getClassLoader();
    }
}
