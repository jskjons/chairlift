package com.rei.chairlift;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;

public class TestUtils {
    public static void createTemplateZip(String testTemplateName, Path dest) throws Exception {
        URL url = TestUtils.class.getClassLoader().getResource(testTemplateName + "/config.groovy");
        Assert.assertNotNull("no template with name " + testTemplateName, url);
        Path folder = Paths.get(url.toURI().resolve("."));
        ZipUtils.create(folder, dest);
    }
}
