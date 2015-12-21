package com.rei.chairlift;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

public class DependencyResolver {
    public static final String userHome = System.getProperty("user.home");
    public static final File userMavenConfigurationHome = new File(userHome, ".m2");
    public static final String envM2Home = System.getenv("M2_HOME");
    public static final File DEFAULT_USER_SETTINGS_FILE = new File(userMavenConfigurationHome, "settings.xml");
    public static final File DEFAULT_GLOBAL_SETTINGS_FILE = new File(
            System.getProperty("maven.home", envM2Home != null ? envM2Home : ""), "conf/settings.xml");
    
    private Settings settings;
    private RepositorySystem repositorySystem;

    public Artifact resolveSingleArtifact(String gavSpec) {
        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(new DefaultArtifact(gavSpec));
        request.setRepositories(getConfiguredRepositories());

        try {
            return getRepositorySystem().resolveArtifact(newRepositorySystemSession(), request).getArtifact();
        } catch (ArtifactResolutionException e) {
            throw new DependencyResolutionException(e);
        }
    }
    
    public List<Artifact> resolveDependencies(Artifact artifact) {
        try {
            DefaultRepositorySystemSession session = newRepositorySystemSession();
            
            ArtifactDescriptorRequest descriptorRequest = new ArtifactDescriptorRequest()
                    .setArtifact(artifact)
                    .setRepositories(getConfiguredRepositories());
            
            ArtifactDescriptorResult descriptorResult = getRepositorySystem().readArtifactDescriptor(session, descriptorRequest);
            
            CollectRequest request = new CollectRequest()
                    .setRoot(new Dependency(artifact, JavaScopes.RUNTIME))
                    .setDependencies(descriptorResult.getDependencies())
                    .setManagedDependencies(descriptorResult.getManagedDependencies())
                    .setRepositories(getConfiguredRepositories());
    
            DependencyRequest dependencyRequest = new DependencyRequest(request, DependencyFilterUtils.classpathFilter(JavaScopes.RUNTIME));
    
            return getRepositorySystem().resolveDependencies(session, dependencyRequest).getArtifactResults().stream()
                                        .map(r -> r.getArtifact())
                                        .collect(toList());
            
        } catch (ArtifactDescriptorException | org.eclipse.aether.resolution.DependencyResolutionException e) {
            throw new DependencyResolutionException(e);
        }
    }
    
    private RepositorySystem getRepositorySystem() {
        if (repositorySystem == null) {
            DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
            locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
            locator.addService(TransporterFactory.class, FileTransporterFactory.class);
            locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
            
            locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
                @Override
                public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable e) {
                    throw new IllegalStateException(e);
                }
            });
            
            repositorySystem = locator.getService(RepositorySystem.class);
        }
        return repositorySystem;
    }

    private DefaultRepositorySystemSession newRepositorySystemSession() {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        
        LocalRepository localRepo = new LocalRepository(getSettings().getLocalRepository());
        session.setLocalRepositoryManager(getRepositorySystem().newLocalRepositoryManager(session, localRepo));

        // session.setTransferListener(new ConsoleTransferListener());
        // session.setRepositoryListener(new ConsoleRepositoryListener());

        // uncomment to generate dirty trees
        // session.setDependencyGraphTransformer( null );

        return session;
    }

    private List<RemoteRepository> getConfiguredRepositories() {
        Map<String, Profile> profilesMap = getSettings().getProfilesAsMap();
        List<RemoteRepository> remotes = new ArrayList<>();

        for (String profileName : getSettings().getActiveProfiles()) {
            Profile profile = profilesMap.get(profileName);
            List<org.apache.maven.settings.Repository> repositories = profile.getRepositories();
            for (org.apache.maven.settings.Repository repo : repositories) {
                remotes.add(new RemoteRepository.Builder(repo.getId(), "default", repo.getUrl()).build());
            }
        }

        return remotes;
    }

    private Settings getSettings() {
        if (settings == null) {
            try {
                SettingsBuildingRequest settingsBuildingRequest = new DefaultSettingsBuildingRequest()
                        .setSystemProperties(System.getProperties())
                        .setUserSettingsFile(DEFAULT_USER_SETTINGS_FILE)
                        .setGlobalSettingsFile(DEFAULT_GLOBAL_SETTINGS_FILE);

                settings = new DefaultSettingsBuilderFactory().newInstance().build(settingsBuildingRequest).getEffectiveSettings();
            } catch (SettingsBuildingException e) {
                throw new IllegalStateException(e);
            }
        }
        return settings;
    }
    
    public static class DependencyResolutionException extends RuntimeException {
        public DependencyResolutionException(Throwable t) {
            super(t);
        }
    }
}
