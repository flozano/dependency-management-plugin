package io.spring.gradle.dependencymanagement;

import io.spring.gradle.dependencymanagement.exclusions.Exclusions;
import io.spring.gradle.dependencymanagement.maven.EffectiveModelBuilder;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Container object for a Gradle build project's dependency management, handling the project's
 * global and configuration-specific dependency management
 *
 * @author Andy Wilkinson
 */
public class DependencyManagementContainer {

    private final Logger log = LoggerFactory.getLogger(DependencyManagementContainer.class);

    private final DependencyManagementConfigurationContainer configurationContainer;

    private final DependencyManagement globalDependencyManagement;

    private final EffectiveModelBuilder effectiveModelBuilder;

    private final Project project;

    private final Map<Configuration, DependencyManagement> configurationDependencyManagement = new LinkedHashMap<Configuration, DependencyManagement>();

    public DependencyManagementContainer(Project project,
            DependencyManagementConfigurationContainer configurationContainer,
            EffectiveModelBuilder effectiveModelBuilder) {
        this.project = project;
        this.configurationContainer = configurationContainer;
        this.globalDependencyManagement = new DependencyManagement(this.project,
                configurationContainer.newConfiguration(), effectiveModelBuilder);
        this.effectiveModelBuilder = effectiveModelBuilder;
    }

    public Project getProject() {
        return project;
    }

    public void addImplicitManagedVersion(Configuration configuration, String group, String name,
            String version) {
        dependencyManagementForConfiguration(configuration)
                .addImplicitManagedVersion(group, name, version);
    }

    public void addExplicitManagedVersion(Configuration configuration, String group, String name,
            String version, List<String> exclusions) {
        dependencyManagementForConfiguration(configuration)
                .addExplicitManagedVersion(group, name, version, exclusions);
    }

    public void importBom(Configuration configuration, String coordinates,
            Map<String, String> properties) {
        dependencyManagementForConfiguration(configuration).importBom(coordinates, properties);
    }

    public String getManagedVersion(Configuration configuration, String group, String name) {
        String version = null;
        if (configuration != null) {
            version = findManagedVersion(configuration, group, name);
        }
        if (version == null) {
            version = globalDependencyManagement.getManagedVersion(group, name);
            if (version != null) {
                log.debug(
                        "Found managed version '{}' for dependency '{}:{}' in global dependency "
                                + "management", version, group, name);
            }
        }
        return version;
    }

    private String findManagedVersion(Configuration source, String group, String name) {
        for (Configuration configuration : source.getHierarchy()) {
            String managedVersion = dependencyManagementForConfiguration(configuration)
                    .getManagedVersion(group, name);
            if (managedVersion != null) {
                log.debug(
                        "Found managed version '{}' for dependency '{}:{}' in dependency "
                                + "management for configuration '{}'", managedVersion, group, name);
                return managedVersion;
            }
        }
        return null;
    }

    public Exclusions getExclusions(Configuration source) {
        Exclusions exclusions = new Exclusions();
        if (source != null) {
            for (Configuration configuration : source.getHierarchy()) {
                exclusions.addAll(dependencyManagementForConfiguration(configuration)
                        .getExclusions());
            }

        }
        exclusions.addAll(globalDependencyManagement.getExclusions());
        return exclusions;
    }

    public Properties importedPropertiesForConfiguration(Configuration source) {
        Properties properties = new Properties();
        properties.putAll(globalDependencyManagement.getImportedProperties());
        if (source != null) {
            for (Configuration configuration : getReversedHierarchy(source)) {
                properties.putAll(dependencyManagementForConfiguration(configuration)
                        .getImportedProperties());
            }

        }
        return properties;
    }

    public Map<String, String> managedVersionsForConfiguration(Configuration configuration) {
        return managedVersionsForConfiguration(configuration, true);
    }

    public Map<String, String> managedVersionsForConfiguration(Configuration source,
            boolean inherited) {
        if (inherited) {
            Map<String, String> managedVersions = dependencyManagementForConfiguration(null)
                    .getManagedVersions();
            if (source != null) {
                for (Configuration configuration : getReversedHierarchy(source)) {
                    managedVersions.putAll(dependencyManagementForConfiguration(configuration)
                            .getManagedVersions());
                }
            }
            return managedVersions;
        }
        return dependencyManagementForConfiguration(source).getManagedVersions();
    }

    private List<Configuration> getReversedHierarchy(Configuration configuration) {
        List<Configuration> hierarchy = new ArrayList<Configuration>(configuration.getHierarchy());
        Collections.reverse(hierarchy);
        return hierarchy;
    }

    private DependencyManagement dependencyManagementForConfiguration(Configuration configuration) {
        if (configuration == null) {
            return globalDependencyManagement;
        }
        else {
            DependencyManagement dependencyManagement = configurationDependencyManagement
                    .get(configuration);
            if (dependencyManagement == null) {
                Configuration dependencyManagementConfiguration = this.configurationContainer
                        .newConfiguration();
                dependencyManagement = new DependencyManagement(this.project, configuration,
                        dependencyManagementConfiguration, this.effectiveModelBuilder);
                this.configurationDependencyManagement.put(configuration, dependencyManagement);
            }
            return dependencyManagement;
        }

    }
}
