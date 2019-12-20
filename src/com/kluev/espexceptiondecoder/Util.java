package com.kluev.espexceptiondecoder;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

public class Util {
    public static ArrayList<Path> glob(String glob, String location) throws IOException {

        final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(glob);

        ArrayList<Path> paths = new ArrayList<Path>();
        Files.walkFileTree(Paths.get(location), new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path path,
                                             BasicFileAttributes attrs) throws IOException {
                if (pathMatcher.matches(path)) {
                    paths.add(path);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc)
                    throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });

        return paths;
    }
}
