package com.rei.chairlift.testing;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.maven.cli.MavenCli;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.Assert;

import com.rei.chairlift.Chairlift;
import com.rei.chairlift.ChairliftConfig;
import com.rei.chairlift.TemplateConfig;
import com.rei.chairlift.util.ZipUtils;

public class TemplateTest {
    private static final String RELATIVE_POM_LOCATION = "../../pom.xml";
    private Path srcFolder;
    private Path tmp;
    private Map<String, String> params = new LinkedHashMap<>();
    private Path destFolder;
    private String[] goals;
    private Map<String, Predicate<String>> validations = new LinkedHashMap<>();
    
    public TemplateTest(Path folder) throws IOException {
        srcFolder = folder;
        tmp = Files.createTempDirectory("chairlift-templ-test");
        destFolder = tmp.resolve("project");
    }
    
    public TemplateTest withParams(Map<String, String> params) {
        this.params.putAll(params);
        return this;
    }
    
    public TemplateTest withParam(String name, String value) {
        params.put(name, value);
        return this;
    }    
    
    public TemplateTest runsMavenGoals(String... goals) {
        this.goals = goals;
        return this;
    }    
    
    public TemplateTest runsMavenPackage() {
        goals = new String[] {"package"};
        return this;
    }    
    
    public TemplateTest generatesFile(String path) {
        validations.put(path, s -> true);
        return this;
    }
    
    public TemplateTest generatesFileWithContent(String path, String content) {
        return generatesFileWithContent(path, s -> s.equals(content));
    }
    
    public TemplateTest generatesFileWithContent(String path, Predicate<String> validator) {
        validations.put(path, validator);
        return this;
    }
    
    public void execute() throws Exception {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(Files.newBufferedReader(srcFolder.resolve(RELATIVE_POM_LOCATION)));
        Artifact artifact = new DefaultArtifact(model.getGroupId(), model.getArtifactId(), model.getPackaging(), model.getVersion());
        Path templateJar = tmp.resolve("template.jar");
        ZipUtils.create(srcFolder, templateJar);
        artifact = artifact.setFile(templateJar.toFile());
     
        Chairlift chairlift = new Chairlift(new ChairliftConfig(false, params));
        chairlift.generate(artifact, destFolder);
        
        validations.forEach((path, validator) -> {
            Assert.assertTrue("expected " + path + " to exist", Files.exists(destFolder.resolve(path)));
            try {
                String content = new String(Files.readAllBytes(destFolder.resolve(path)));
                Assert.assertTrue(path + " didn't pass validation!", validator.test(content));
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        });
        
        if (goals != null && goals.length > 0) {
            MavenCli cli = new MavenCli();
            cli.doMain(goals, destFolder.toAbsolutePath().toString(), System.out, System.out);
        }
        
        
    }

    public static TemplateTest forCurrentProject() {
        try {
            URL url = TemplateTest.class.getClassLoader().getResource(TemplateConfig.CONFIG_GROOVY);
            Assert.assertNotNull("no config.groovy exists on the classpath!", url);
            Path folder = Paths.get(url.toURI().resolve("."));
            return new TemplateTest(folder);
        } catch (URISyntaxException | IOException e) {
            throw new AssertionError("unable to create template! ", e);
        }
    }
}
