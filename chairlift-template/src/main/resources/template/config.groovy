param('groupId', "groupId of project", "com.rei.chairlift")
param('artifactId', "artifactId of project", "chairlift-template")

params.pkg = params.groupId
params.pkgPath = params.pkg.replace('.', '/')
params.testClass = toCamelCase(params.artifactId) + 'Test'