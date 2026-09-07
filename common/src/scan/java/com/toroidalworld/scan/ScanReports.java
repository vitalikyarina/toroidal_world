package com.toroidalworld.scan;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class ScanReports {
    static final Path DIRECTORY =
            Path.of(System.getProperty("toroidal.reports", "build/reports")).resolve("scan");

    static void write(Path path, String report) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, report);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void write(Path path, List<String> lines) {
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, lines);
        } catch (IOException failed) {
            throw new UncheckedIOException(failed);
        }
    }

    private ScanReports() {
    }
}
