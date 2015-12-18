package com.rei.chairlift;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Collections;

import org.eclipse.aether.artifact.Artifact;

public class TemplateArchive {
    private FileSystem zipFs;
    private Artifact artifact;

    public TemplateArchive(Artifact artifact) throws IOException {
        this.artifact = artifact;
        final URI uri = URI.create("jar:file:" + artifact.getFile().toURI().getPath());
        zipFs = FileSystems.newFileSystem(uri, Collections.emptyMap());
    }
    
    public String read(String path) throws IOException {
        return new String(Files.readAllBytes(zipFs.getPath(path)));
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
