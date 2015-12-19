package com.rei.chairlift;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.aether.artifact.Artifact;

public class TemplateArchive {
    
    private Artifact artifact;
    protected FileSystem fileSystem;

    public TemplateArchive(Artifact artifact) {
        this.artifact = artifact;
    }

    public void init() throws IOException {
        if (fileSystem != null) {
            return; //already initialized
        }
        final URI uri = URI.create("jar:file:" + artifact.getFile().toURI().getPath());
        fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
    }

    public Optional<String> read(String path) throws IOException {
        init();
        Path p = fileSystem.getPath(path);
        if (Files.exists(p)) {
            return Optional.of(new String(Files.readAllBytes(p)));
        }
        
        return Optional.empty();
    }
    
    public List<String> list(String prefix) throws IOException {
        init();
        return Files.list(fileSystem.getPath("/"))
                    .map(p -> p.getFileName().toString())
                    .filter(n -> n.startsWith(prefix))
                    .collect(toList());
    }

    public String getVersion() {
        return artifact.getVersion();
    }

    public String getGroupId() {
        return artifact.getGroupId();
    }

    public String getArtifactId() {
        return artifact.getArtifactId();
    }

    public String getClassifier() {
        return artifact.getClassifier();
    }

    public String getExtension() {
        return artifact.getExtension();
    }

    public void unpackTo(Path projectDir, 
                         List<Predicate<Path>> copyFilters, 
                         List<Predicate<Path>> processingFilters, 
                         Function<Path, Path> filenameTransformer, 
                         Function<String, String> contentTransformer) throws IOException {
        
        // if the destination doesn't exist, create it
        if (Files.notExists(projectDir)) {
            Files.createDirectories(projectDir);
        }

        final Path root = fileSystem.getPath("/");

        // walk the zip file tree and copy files to the destination
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (allMatch(copyFilters, file)) {
                    String content = new String(Files.readAllBytes(file));
                    content = allMatch(processingFilters, file) ? contentTransformer.apply(content) : content;
                    
                    final Path dest = filenameTransformer.apply(projectDir.resolve(file.toString().substring(1)));
                    Files.write(dest, content.getBytes());
                }
                
                return FileVisitResult.CONTINUE;
            }

            private boolean allMatch(List<Predicate<Path>> copyFilters, Path file) {
                return copyFilters.stream().allMatch(p -> p.test(file));
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path dest = Paths.get(projectDir.toString(), dir.toString());
                Files.createDirectories(filenameTransformer.apply(dest));
                return FileVisitResult.CONTINUE;
            }
        });
    }

}
