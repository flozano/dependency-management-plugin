package io.spring.gradle.dependencymanagement.report;

import io.spring.gradle.dependencymanagement.DependencyManagementContainer;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Task to display the dependency management for a project.
 *
 * @author Andy Wilkinson.
 */
public class DependencyManagementReportTask extends DefaultTask {

    private DependencyManagementContainer dependencyManagement;

    private DependencyManagementReportRenderer renderer;

    @Inject
    public DependencyManagementReportTask() {
        this.renderer = new DependencyManagementReportRenderer();
    }

    public void setDependencyManagement(DependencyManagementContainer dependencyManagement) {
        this.dependencyManagement = dependencyManagement;
    }

    void setRenderer(DependencyManagementReportRenderer renderer) {
        this.renderer = renderer;
    }

    @TaskAction
    public void report() {
        this.renderer.startProject(getProject());

        Map<String, String> globalManagedVersions =
                this.dependencyManagement.managedVersionsForConfiguration(null);

        this.renderer.renderGlobalManagedVersions(globalManagedVersions);

        Set<Configuration> configurations = new TreeSet<Configuration>(
                new Comparator<Configuration>() {
                        @Override
                        public int compare(Configuration one, Configuration two) {
                            return one.getName().compareTo(two.getName());
                        }
                }
        );
        configurations.addAll(getProject().getConfigurations());
        for (Configuration configuration: configurations) {
            Map<String, String> managedVersions =
                    this.dependencyManagement.managedVersionsForConfiguration(configuration);
            this.renderer.renderConfigurationManagedVersions(managedVersions, configuration,
                    globalManagedVersions);
        }
    }

}
