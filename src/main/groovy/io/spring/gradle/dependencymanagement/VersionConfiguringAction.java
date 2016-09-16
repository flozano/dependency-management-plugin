package io.spring.gradle.dependencymanagement;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencyResolveDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * An {@link Action} to be applied to {@link DependencyResolveDetails} that configures the
 * dependency's version based on the dependency management.
 *
 * @author Andy Wilkinson
 */
public class VersionConfiguringAction implements Action<DependencyResolveDetails> {

    private static final Logger log = LoggerFactory.getLogger(VersionConfiguringAction.class);

    private final Project project;

    private final DependencyManagementContainer dependencyManagementContainer;

    private final Configuration configuration;

    public VersionConfiguringAction(Project project,
            DependencyManagementContainer dependencyManagementContainer,
            Configuration configuration) {
        this.project = project;
        this.dependencyManagementContainer = dependencyManagementContainer;
        this.configuration = configuration;
    }

    @Override
    public void execute(DependencyResolveDetails details) {
        this.log.debug("Processing dependency '{}'", details.getRequested());
        if (isDependencyOnLocalProject(project, details)) {
            this.log.debug("'{}' is a local project dependency. Dependency management has not " +
                    "been applied", details.getRequested());
            return;
        }

        if (Versions.isDynamic(details.getRequested().getVersion())) {
            this.log.debug("'{}' has a dynamic version. Dependency management has not been applied",
                    details.getRequested());
            return;

        }

        String version = dependencyManagementContainer
                .getManagedVersion(configuration, details.getRequested().getGroup(),
                        details.getRequested().getName());

        if (version != null) {
            this.log.info("Using version '{}' for dependency '{}'", version,
                    details.getRequested());
            details.useVersion(version);
        }
        else {
            this.log.debug("No dependency management for dependency '{}'", details.getRequested());
        }

    }

    private boolean isDependencyOnLocalProject(Project project,
            DependencyResolveDetails details) {
        return getAllProjectNames(project.getRootProject()).contains(details.getRequested()
                .getGroup() + ":" + details.getRequested().getName());
    }

    private Set<String> getAllProjectNames(Project rootProject) {
        Set<String> names = new HashSet<String>();
        for (Project project: rootProject.getAllprojects()) {
            names.add(project.getGroup() + ":" + project.getName());
        }
        return names;
    }

}
