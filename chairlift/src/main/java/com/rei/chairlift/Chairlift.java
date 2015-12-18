package com.rei.chairlift;

import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.aether.artifact.Artifact;

public class Chairlift {
    public Chairlift(ChairliftConfig globalConfig) {
    }
    
    public void generate(Artifact templateArtifact, Path dest) throws IOException {
        TemplateArchive archive = new TemplateArchive(templateArtifact.getFile().toPath());
        TemplateConfig.loadFrom(archive);
    }

}
