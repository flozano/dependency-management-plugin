package io.spring.gradle.dependencymanagement;

import io.spring.gradle.dependencymanagement.exclusions.ExclusionConfiguringAction;
import io.spring.gradle.dependencymanagement.exclusions.ExclusionResolver;
import io.spring.gradle.dependencymanagement.maven.EffectiveModelBuilder;
import io.spring.gradle.dependencymanagement.report.DependencyManagementReportTask;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.XmlProvider;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.artifacts.maven.MavenDeployer;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.tasks.Upload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for the dependency management plugin
 *
 * @author Andy Wilkinson
 */
public class DependencyManagementPlugin implements Plugin<Project> {
    @Override
    public void apply(final Project project) {
        DependencyManagementConfigurationContainer configurationContainer = new DependencyManagementConfigurationContainer(
                project);

        EffectiveModelBuilder effectiveModelBuilder = new EffectiveModelBuilder(project,
                configurationContainer);

        final DependencyManagementContainer dependencyManagementContainer = new DependencyManagementContainer(
                project, configurationContainer, effectiveModelBuilder);

        final DependencyManagementExtension extension = project.getExtensions()
                .create("dependencyManagement", DependencyManagementExtension.class,
                        dependencyManagementContainer, configurationContainer, project);

        project.getTasks().create("dependencyManagement", DependencyManagementReportTask.class,
                new Action<DependencyManagementReportTask>() {
                    @Override
                    public void execute(DependencyManagementReportTask dependencyManagementReportTask) {
                        dependencyManagementReportTask.setDependencyManagement(dependencyManagementContainer);
                    }
                });

        if (extension.isOverriddenByDependencies()) {
            project.getConfigurations()
                    .all(new ImplicitDependencyManagementCollector(dependencyManagementContainer));
        }


        ExclusionResolver exclusionResolver = new ExclusionResolver(project.getDependencies(),
                configurationContainer, effectiveModelBuilder);

        project.getConfigurations().all(new DependencyManagementApplier(project, exclusionResolver,
                dependencyManagementContainer, configurationContainer));

        project.getTasks().withType(Upload.class, new Action<Upload>() {
            @Override
            public void execute(Upload upload) {

                upload.getRepositories().withType(MavenDeployer.class, new Action<MavenDeployer>() {
                    @Override
                    public void execute(MavenDeployer mavenDeployer) {
                        mavenDeployer.getPom().withXml(extension.getPomConfigurer());
                    }
                });

            }

        });

        project.getPlugins().withType(MavenPublishPlugin.class, new Action<MavenPublishPlugin>() {
            @Override
            public void execute(MavenPublishPlugin mavenPublishPlugin) {
                project.getExtensions()
                        .configure(PublishingExtension.class, new Action<PublishingExtension>() {
                            @Override
                            public void execute(PublishingExtension publishingExtension) {
                                publishingExtension.getPublications()
                                        .withType(MavenPublication.class,
                                                new Action<MavenPublication>() {
                                                    @Override
                                                    public void execute(
                                                            MavenPublication publication) {
                                                        publication.getPom().withXml(
                                                                (Action<? super XmlProvider>) extension
                                                                        .getPomConfigurer());
                                                    }

                                                });
                            }

                        });
            }

        });

    }

    private final Logger log = LoggerFactory.getLogger(DependencyManagementPlugin.class);

    /**
     * An {@link Action} that adds an implict managed versions to the dependency management for each
     * of the {@link Configuration Configuration's} dependencies that has a version that is not
     * dynamic.
     */
    private static class ImplicitDependencyManagementCollector implements Action<Configuration> {
        public ImplicitDependencyManagementCollector(
                DependencyManagementContainer dependencyManagementContainer) {
            this.dependencyManagementContainer = dependencyManagementContainer;
        }

        @Override
        public void execute(final Configuration root) {
            root.getIncoming().beforeResolve(new Action<ResolvableDependencies>() {
                @Override
                public void execute(ResolvableDependencies resolvableDependencies) {
                    for (Configuration configuration : root.getHierarchy()) {
                        for (Dependency dependency : configuration.getIncoming()
                                .getDependencies()) {
                            if (dependency instanceof ModuleDependency && dependency
                                    .getVersion() != null) {
                                if (Versions.isDynamic(dependency.getVersion())) {
                                    log.debug(
                                            "Dependency '{}' in configuration '{}' has a dynamic " + "version. The version will not be added to the managed " + "versions",
                                            dependency, configuration.getName());
                                }
                                else {
                                    log.debug(
                                            "Adding managed version in configuration '{}' for " + "dependency '{}'",
                                            configuration.getName(), dependency);
                                    dependencyManagementContainer
                                            .addImplicitManagedVersion(configuration,
                                                    dependency.getGroup(), dependency.getName(),
                                                    dependency.getVersion());
                                }

                            }

                        }

                    }

                }

            });
        }

        private static final Logger log = LoggerFactory
                .getLogger(ImplicitDependencyManagementCollector.class);
        private final DependencyManagementContainer dependencyManagementContainer;
    }

    private static class DependencyManagementApplier implements Action<Configuration> {
        public DependencyManagementApplier(Project project, ExclusionResolver exclusionResolver,
                DependencyManagementContainer dependencyManagementContainer,
                DependencyManagementConfigurationContainer configurationContainer) {
            this.project = project;
            this.exclusionResolver = exclusionResolver;
            this.dependencyManagementContainer = dependencyManagementContainer;
            this.configurationContainer = configurationContainer;
        }

        @Override
        public void execute(Configuration configuration) {
            log.info("Applying dependency management to configuration '{}' in project '{}'",
                    configuration.getName(), this.project.getName());

            VersionConfiguringAction versionConfiguringAction = new VersionConfiguringAction(
                    this.project, this.dependencyManagementContainer, configuration);

            configuration.getIncoming().beforeResolve(new ExclusionConfiguringAction(
                    this.project.getExtensions().findByType(DependencyManagementExtension.class),
                    dependencyManagementContainer, configurationContainer, configuration,
                    exclusionResolver, versionConfiguringAction));

            configuration.getResolutionStrategy().eachDependency(versionConfiguringAction);
        }

        private static final Logger log = LoggerFactory
                .getLogger(DependencyManagementApplier.class);
        private final Project project;
        private final ExclusionResolver exclusionResolver;
        private final DependencyManagementContainer dependencyManagementContainer;
        private final DependencyManagementConfigurationContainer configurationContainer;
    }
}
