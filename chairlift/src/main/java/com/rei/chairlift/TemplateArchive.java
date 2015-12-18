package com.rei.chairlift;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Collections;

import org.eclipse.aether.artifact.Artifact;

public class TemplateArchive {
    private Artifact artifact;
    protected FileSystem fileSystem;

    public TemplateArchive(Artifact artifact) {
        this.artifact = artifact;
    }
    
    public void init() throws IOException {
        final URI uri = URI.create("jar:file:" + artifact.getFile().toURI().getPath());
        fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
    }
    
    public String read(String path) throws IOException {
        return new String(Files.readAllBytes(fileSystem.getPath(path)));
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
    
}
