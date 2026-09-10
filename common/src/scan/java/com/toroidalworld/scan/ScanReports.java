package com.toroidalworld.scan;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class ScanReports {
    static final Path DIRECTORY =
            Path.of(System.getProperty("toroidal.reports", "build/reports")).resolve("scan");

    private static final String POPULATION_PREFIX = "population: ";

    private static final String REASON_DASH = "—";

    private static final String PROGRESS_SUFFIX = "-progress.txt";

    private static final String PROGRESS_TAG = "[toroidal-scan]";

    private static final DateTimeFormatter PROGRESS_CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final String VALUE_FORMAT = "%.3f";

    private static final Map<String, Path> PROGRESS = new ConcurrentHashMap<>();

    static void write(Path path, String population, String report) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, population + System.lineSeparator() + System.lineSeparator() + report);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void write(Path path, String population, List<String> lines) {
        List<String> headed = new ArrayList<>();
        headed.add(population);
        headed.add("");
        headed.addAll(lines);
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, headed);
        } catch (IOException failed) {
            throw new UncheckedIOException(failed);
        }
    }

    static String population(int seeds, long base, long step, String reason) {
        return POPULATION_PREFIX + seeds + " seeds from " + base + ", step 0x" + Long.toHexString(step)
                + " " + REASON_DASH + " " + reason;
    }

    static String noPopulation(String reason) {
        return POPULATION_PREFIX + "none " + REASON_DASH + " " + reason;
    }

    static void note(String scan, String name, String keys) {
        Path path = PROGRESS.computeIfAbsent(scan, ScanReports::freshProgress);
        String line = PROGRESS_TAG + " " + name + " at=" + LocalTime.now().format(PROGRESS_CLOCK) + " " + keys
                + System.lineSeparator();
        synchronized (PROGRESS) {
            try {
                Files.writeString(path, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
            } catch (IOException failed) {
                throw new UncheckedIOException(failed);
            }
        }
    }

    static String value(double number) {
        return String.format(Locale.ROOT, VALUE_FORMAT, number);
    }

    private static Path freshProgress(String scan) {
        Path path = DIRECTORY.resolve(scan + PROGRESS_SUFFIX);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, "");
        } catch (IOException failed) {
            throw new UncheckedIOException(failed);
        }

        return path;
    }

    private ScanReports() {
    }
}
