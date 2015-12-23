package com.rei.chairlift.testing;

import static com.rei.chairlift.TemplateConfig.CONFIG_GROOVY;
import static com.rei.chairlift.TemplateConfig.DEFAULT_TEMPLATE;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.maven.cli.MavenCli;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.Assert;
import org.junit.rules.ExternalResource;
import org.slf4j.impl.SimpleLogger;

import com.rei.chairlift.Chairlift;
import com.rei.chairlift.ChairliftConfig;
import com.rei.chairlift.util.ZipUtils;

public class TemplateTester extends ExternalResource {
    private static final String RELATIVE_POM_LOCATION = "../../pom.xml";
    private Path srcFolder;
    private Path tmp;
    private Path destFolder;
    private String groupId;
    private String version;
    private boolean deleteOnFail = true;
    private boolean failed = false;
    
    private TemplateTester(Path folder) throws IOException {
        srcFolder = folder;
    }
    
    public static TemplateTester forCurrentProject() {
        try {
            URL url = TemplateTester.class.getClassLoader().getResource(DEFAULT_TEMPLATE + "/" + CONFIG_GROOVY);
            Assert.assertNotNull("no config.groovy exists on the classpath!", url);
            Path folder = Paths.get(url.toURI().resolve(".."));
            return new TemplateTester(folder);
        } catch (URISyntaxException | IOException e) {
            throw new AssertionError("unable to create template! ", e);
        }
    }
    
    @Override
    protected void before() throws Throwable {
        tmp = Files.createTempDirectory(srcFolder.resolve("..").normalize(), "chairlift-templ-test");
        destFolder = tmp.resolve("project");
        
    }
    
    @Override
    protected void after() {
        if (!deleteOnFail && failed) { return; }
        
        try {
            FileUtils.deleteDirectory(tmp.toFile());
        } catch (IOException e) {
            System.out.println("unable to delete temp dir: " + e.getMessage());
        }
    }
    
    public TemplateTester deleteOnFailure(boolean deleteOnFail) {
        this.deleteOnFail = deleteOnFail;
        return this;
    }
    
    public TemplateTester useGroupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public TemplateTester useVersion(String version) {
        this.version = version;
        return this;
    }

    public TemplateTester debugLogging() {
        System.setProperty(SimpleLogger.LOG_KEY_PREFIX + "com.rei.chairlift", "debug");
        return this;
    }
    
    public TestScenario forTemplate() {
        return new TestScenario();
    }
    
    public TestScenario forSubTemplate(String name) {
        TestScenario scenario = new TestScenario();
        scenario.subtemplate = name;
        return scenario;
    }
    
    public class TestScenario {
        private Map<String, String> params = new LinkedHashMap<>();
        private String[] goals;
        private Map<String, Validation> validations = new LinkedHashMap<>();
        private List<String> doesntGenerate = new LinkedList<>();
        private String subtemplate;
        
        public TestScenario withParams(Map<String, String> params) {
            this.params.putAll(params);
            return this;
        }
        
        public TestScenario withParam(String name, String value) {
            params.put(name, value);
            return this;
        }    
        
        public TestScenario runsMavenGoals(String... goals) {
            this.goals = goals;
            return this;
        }    
        
        public TestScenario runsMavenPackage() {
            goals = new String[] {"package"};
            return this;
        }    
        
        public TestScenario generatesFile(String path) {
            validations.put(path, new Validation("", s -> true));
            return this;
        }
        
        public TestScenario generatesFileNotContaining(String path, String contentSubStr) {
            return generatesFileWithContent(path, "shouldn't contain " + contentSubStr, s -> !s.contains(contentSubStr)); 
        }
        
        public TestScenario generatesFileContaining(String path, String contentSubStr) {
            return generatesFileWithContent(path, "should contain " + contentSubStr, s -> s.contains(contentSubStr)); 
        }
        
        public TestScenario generatesFileWithContent(String path, String content) {
            return generatesFileWithContent(path, "should have content " + content, s -> s.equals(content));
        }
        
        public TestScenario generatesFileWithContent(String path, Predicate<String> validator) {
            return generatesFileWithContent(path, "predicate failed!", validator);
        }
        
        public TestScenario generatesFileWithContent(String path, String message, Predicate<String> validator) {
            validations.put(path, new Validation(message, validator));
            return this;
        }
        
        public TestScenario doesNotGenerate(String path) {
            doesntGenerate.add(path);
            return this;
        }
        
        public void generateAndValidate() throws Exception {
            try {
                doGenerateAndValidate();
            } catch (Throwable t) {
                failed = true;
                throw t;
            }
        }
        
        private void doGenerateAndValidate() throws Exception {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(Files.newBufferedReader(srcFolder.resolve(RELATIVE_POM_LOCATION)));
            Artifact artifact = new DefaultArtifact(groupId != null ? groupId : model.getGroupId(), 
                                                    model.getArtifactId(), 
                                                    model.getPackaging(), 
                                                    version != null ? version : model.getVersion());
            
            Path templateJar = tmp.resolve("template.jar");
            ZipUtils.create(srcFolder, templateJar);
            artifact = artifact.setFile(templateJar.toFile());
            
            Chairlift chairlift = new Chairlift(new ChairliftConfig(false, false, params));
            chairlift.generate(artifact, subtemplate, destFolder);
            
            validations.forEach((path, validation) -> {
                Assert.assertTrue("expected " + path + " to exist", Files.exists(destFolder.resolve(path)));
                try {
                    String content = new String(Files.readAllBytes(destFolder.resolve(path)));
                    Assert.assertTrue(path + " didn't pass validation! " + validation.message, validation.validator.test(content));
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            });
            
            doesntGenerate.forEach(path -> 
                Assert.assertFalse("expected " + path + " NOT to exist!", Files.exists(destFolder.resolve(path))));
            
            if (goals != null && goals.length > 0) {
                System.setProperty(MavenCli.MULTIMODULE_PROJECT_DIRECTORY, destFolder.toAbsolutePath().toString());
                MavenCli cli = new MavenCli();
                int returnCode = cli.doMain(goals, destFolder.toAbsolutePath().toString(), System.out, System.out);
                Assert.assertEquals("maven command failed!", 0, returnCode);
            }            
        }
    }
    
    private static class Validation {
        String message;
        Predicate<String> validator;
        
        public Validation(String message, Predicate<String> validator) {
            this.message = message;
            this.validator = validator;
        }
    }
}
