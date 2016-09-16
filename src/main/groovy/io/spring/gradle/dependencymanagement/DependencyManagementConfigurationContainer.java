package io.spring.gradle.dependencymanagement;

import org.gradle.api.Action;
import org.gradle.api.DomainObjectCollection;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;

/**
 * A container for {@link Configuration Configurations} created by the dependency management plugin
 * that aren't part of the project's configurations.
 *
 * @author Andy Wilkinson
 */
public class DependencyManagementConfigurationContainer {

    private final DomainObjectCollection<Configuration> configurations;

    private final ConfigurationContainer delegate;

    DependencyManagementConfigurationContainer(Project project) {
        this.delegate = project.getConfigurations();
        this.configurations = project.container(Configuration.class);
    }

    public Configuration newConfiguration(Dependency... dependencies) {
        return this.newConfiguration(null, dependencies);
    }

    public Configuration newConfiguration(ConfigurationConfigurer configurer,
            Dependency... dependencies) {
        Configuration configuration = delegate.detachedConfiguration(dependencies);
        if (configurer != null) {
            configurer.configure(configuration);
        }
        this.configurations.add(configuration);
        return configuration;
    }

    void resolutionStrategy(Action<Configuration> action) {
        this.configurations.all(action);
    }

    public interface ConfigurationConfigurer {

        void configure(Configuration configuration);

    }

}
