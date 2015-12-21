# Writing a Chairlift Template

Chairlift template projects are standard jar projects with some special folders in `src/main/resources/`.

The main template must go in the `template/` sub-folder. Sub-templates must go in `subtemplate-\${name}/`.

Each template or sub-template **MUST** contain `config.groovy` and may optionally contain `postinstall.groovy`. 

## config.groovy

The `config.groovy` file contains the configuration for the template. In it you can configure parameters with the 
`param('paramName', 'description/prompt text', defaultValue)` method. You can access parameters with the `params` map.

## Testing your template

It's recommended to test your template by including the chairlift-testing dependency:

    <dependency>
        <groupId>com.rei.chairlift</groupId>
        <artifactId>chairlift-testing</artifactId>
        <version>\${chairliftVersion}</version>
    </dependency>

