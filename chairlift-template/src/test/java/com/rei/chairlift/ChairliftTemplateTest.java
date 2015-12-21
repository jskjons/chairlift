package com.rei.chairlift;

import org.junit.Rule;
import org.junit.Test;

import com.rei.chairlift.testing.TemplateTester;

public class ChairliftTemplateTest {

    @Rule
    public TemplateTester tester = TemplateTester.forCurrentProject();
    
    @Test
    public void canGenerateTemplateAndSubTemplate() throws Exception {
        tester.withParam("groupId", "com.rei.chairlift.testing")
              .generatesFile("src/main/resources/template/config.groovy")
              .generatesFile("src/main/resources/template/postinstall.groovy")
              .generatesFileWithContent("README.md", text -> text.contains("# Writing a Chairlift Template"))
              .runsMavenPackage()
              .generateAndValidate()
              .reset()
              .withParam("name", "my-sub")
              .generatesFile("src/main/resources/subtemplate-my-sub/config.groovy")
              .generatesFile("src/main/resources/subtemplate-my-sub/postinstall.groovy")
              .generateAndValidate("subtemplate");        
    }
    
}
