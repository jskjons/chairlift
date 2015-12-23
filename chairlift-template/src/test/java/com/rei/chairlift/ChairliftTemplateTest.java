package com.rei.chairlift;

import org.junit.Rule;
import org.junit.Test;

import com.rei.chairlift.testing.TemplateTester;

public class ChairliftTemplateTest {

    @Rule
    public TemplateTester tester = TemplateTester.forCurrentProject().deleteOnFailure(false);
    
    @Test
    public void canGenerateTemplateAndSubTemplate() throws Exception {
        tester.forTemplate().withParam("groupId", "com.rei.chairlift.testing")
              .generatesFile("src/main/resources/template/config.groovy")
              .generatesFile("src/main/resources/template/postinstall.groovy")
              .generatesFileContaining("README.md", "# Writing a Chairlift Template")
              .runsMavenPackage()
              .generateAndValidate();
        
        tester.forSubTemplate("subtemplate")
              .withParam("name", "my-sub")
              .generatesFile("src/main/resources/subtemplate-my-sub/config.groovy")
              .generatesFile("src/main/resources/subtemplate-my-sub/postinstall.groovy")
              .generateAndValidate();        
    }
    
}
