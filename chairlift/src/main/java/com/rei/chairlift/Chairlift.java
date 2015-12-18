package com.rei.chairlift;

import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.aether.artifact.Artifact;

public class Chairlift {
    private ChairliftConfig globalConfig;

    public Chairlift(ChairliftConfig globalConfig) {
        this.globalConfig = globalConfig;
    }
    
    public void generate(Artifact templateArtifact, Path projectDir) throws IOException {
        TemplateArchive archive = new TemplateArchive(templateArtifact);
        TemplateConfig config = TemplateConfig.load(archive, globalConfig, projectDir);
        
        
        
    }

}
