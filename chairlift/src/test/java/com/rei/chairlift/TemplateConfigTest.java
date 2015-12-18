package com.rei.chairlift;

import static org.junit.Assert.assertEquals;

import java.nio.file.Path;
import java.util.Map;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.collect.ImmutableMap;

public class TemplateConfigTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();
    
    @Test
    public void testLoad() throws Exception {
        Artifact artifact = new DefaultArtifact("com.rei.test", "simple-template", "zip", "1");
        Path templateZip = tmp.getRoot().toPath().resolve("simple-template.zip");
        TestUtils.createTemplateZip("simple-template", templateZip);
        artifact = artifact.setFile(templateZip.toFile());
        
        TemplateArchive archive = new TemplateArchive(artifact);
        archive.init();
        ChairliftConfig globalConfig = new ChairliftConfig(false, ImmutableMap.of("global", "true"));
        
        TemplateConfig config = TemplateConfig.load(archive, globalConfig, tmp.getRoot().toPath());
        Map<String, Object> params = config.getParameterValues();
        assertEquals(5, params.size());
    }

}
