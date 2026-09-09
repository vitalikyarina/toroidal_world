package com.toroidalworld;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class ApiSurfaceTest {
    private static final String REPO_ROOT_PROPERTY = "toroidal.repoRoot";
    private static final String SOURCE_ROOT = "common/src/main/java";
    private static final String API_PACKAGE = "com.toroidalworld.api";
    private static final String API_PREFIX = API_PACKAGE + ".";
    private static final String MOD_PREFIX = "com.toroidalworld.";
    private static final String JAVA_SUFFIX = ".java";
    private static final String PACKAGE_INFO = "package-info";

    @Test
    void noPublicApiMemberNamesAnInternalType() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Class<?> type : publicApiTypes()) {
            collect(type, violations);
        }

        assertTrue(violations.isEmpty(),
                "api signatures naming a type outside " + API_PACKAGE + ": " + violations.stream().sorted().toList());
    }

    private static void collect(Class<?> type, List<String> violations) {
        String owner = type.getTypeName();
        check(owner, type.getGenericSuperclass(), violations);
        checkAll(owner, type.getGenericInterfaces(), violations);
        checkAll(owner, type.getTypeParameters(), violations);

        for (Field field : type.getDeclaredFields()) {
            if (isSurface(field.getModifiers()) && !field.isSynthetic()) {
                check(owner + "." + field.getName(), field.getGenericType(), violations);
            }
        }
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            if (isSurface(constructor.getModifiers()) && !constructor.isSynthetic()) {
                String where = owner + ".<init>";
                checkAll(where, constructor.getGenericParameterTypes(), violations);
                checkAll(where, constructor.getGenericExceptionTypes(), violations);
            }
        }
        for (Method method : type.getDeclaredMethods()) {
            if (isSurface(method.getModifiers()) && !method.isSynthetic() && !method.isBridge()) {
                String where = owner + "." + method.getName();
                check(where, method.getGenericReturnType(), violations);
                checkAll(where, method.getGenericParameterTypes(), violations);
                checkAll(where, method.getGenericExceptionTypes(), violations);
                checkAll(where, method.getTypeParameters(), violations);
            }
        }
        for (Class<?> nested : type.getDeclaredClasses()) {
            if (isSurface(nested.getModifiers())) {
                collect(nested, violations);
            }
        }
    }

    private static void check(String where, Type type, List<String> violations) {
        switch (type) {
            case null -> {
            }
            case Class<?> raw -> {
                Class<?> element = raw;
                while (element.isArray()) {
                    element = element.getComponentType();
                }
                String name = element.getName();
                if (name.startsWith(MOD_PREFIX) && !name.startsWith(API_PREFIX)) {
                    violations.add(where + " -> " + name);
                }
            }
            case ParameterizedType parameterized -> {
                check(where, parameterized.getRawType(), violations);
                checkAll(where, parameterized.getActualTypeArguments(), violations);
            }
            case GenericArrayType array -> check(where, array.getGenericComponentType(), violations);
            case TypeVariable<?> variable -> checkAll(where, variable.getBounds(), violations);
            case WildcardType wildcard -> {
                checkAll(where, wildcard.getUpperBounds(), violations);
                checkAll(where, wildcard.getLowerBounds(), violations);
            }
            default -> {
            }
        }
    }

    private static void checkAll(String where, Type[] types, List<String> violations) {
        for (Type type : types) {
            check(where, type, violations);
        }
    }

    private static boolean isSurface(int modifiers) {
        return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers);
    }

    private static List<Class<?>> publicApiTypes() throws IOException {
        String repositoryRoot = System.getProperty(REPO_ROOT_PROPERTY);
        assertNotNull(repositoryRoot, "the " + REPO_ROOT_PROPERTY + " system property names no repository root");

        Path root = Path.of(repositoryRoot, SOURCE_ROOT, API_PACKAGE.replace('.', '/'));
        List<Class<?>> types = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.toList()) {
                String fileName = file.getFileName().toString();
                if (!fileName.endsWith(JAVA_SUFFIX) || fileName.equals(PACKAGE_INFO + JAVA_SUFFIX)) {
                    continue;
                }

                Class<?> type = load(root, file);
                if (Modifier.isPublic(type.getModifiers())) {
                    types.add(type);
                }
            }
        }

        assertFalse(types.isEmpty(), "no public type found under " + root);
        return types;
    }

    private static Class<?> load(Path root, Path file) {
        String relative = root.relativize(file).toString().replace('\\', '/');
        String name = API_PREFIX + relative.substring(0, relative.length() - JAVA_SUFFIX.length()).replace('/', '.');
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException notCompiled) {
            throw new AssertionError("api source " + file + " compiles to no class " + name, notCompiled);
        }
    }
}
