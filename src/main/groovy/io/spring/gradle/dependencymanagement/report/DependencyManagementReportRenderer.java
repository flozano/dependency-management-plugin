package io.spring.gradle.dependencymanagement.report;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

import java.io.PrintWriter;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Andy Wilkinson.
 */
class DependencyManagementReportRenderer {

    private final PrintWriter output;

    DependencyManagementReportRenderer() {
        this(new PrintWriter(System.out));
    }

    DependencyManagementReportRenderer(PrintWriter writer) {
        this.output = writer;
    }

    public void startProject(final Project project) {
        output.println();
        output.println("------------------------------------------------------------");
        String heading;
        if (project.getRootProject().equals(project)) {
            heading = "Root project";
        }
        else {
            heading = "Project " + project.getPath();
        }

        if (project.getDescription() != null) {
            heading += " - " + project.getDescription();
        }

        output.println(heading);
        output.println("------------------------------------------------------------");
        output.println();
    }

    public void renderGlobalManagedVersions(Map<String, String> globalManagedVersions) {
        renderDependencyManagementHeader("global",
                "Default dependency management for all configurations");

        if (globalManagedVersions != null && !globalManagedVersions.isEmpty()) {
            renderManagedVersions(globalManagedVersions);
        }
        else {
            output.println("No dependency management");
            output.println();
        }

        output.flush();
    }

    private void renderDependencyManagementHeader(String identifier, String description) {
        output.println(identifier + " - " + description);
    }

    private void renderManagedVersions(Map<String, String> managedVersions) {
        Map<String, String> sortedVersions = new TreeMap<String, String>(new Comparator<String>() {
            @Override
            public int compare(String one, String two) {
                String[] oneComponents = one.split(":");
                String[] twoComponents = two.split(":");
                int result = oneComponents[0].compareTo(twoComponents[0]);
                if (result == 0) {
                    result = oneComponents[1].compareTo(twoComponents[1]);
                }
                return result;
            }
        });
        sortedVersions.putAll(managedVersions);
        for (Map.Entry<String, String> entry: sortedVersions.entrySet()) {
            output.println("    " + entry.getKey() + " " + entry.getValue());
        }
        output.println();
    }

    public void renderConfigurationManagedVersions(Map<String, String> managedVersions,
            final Configuration configuration, Map<String, String> globalManagedVersions) {
        renderDependencyManagementHeader(configuration.getName(),
                "Dependency management for the " + configuration.getName() + " configuration");

        if (managedVersions != null && !managedVersions.isEmpty()) {
            if (!managedVersions.equals(globalManagedVersions)) {
                renderManagedVersions(managedVersions);
            }
            else {
                output.println("No configuration-specific dependency management");
                output.println();
            }

        }
        else {
            output.println("No dependency management");
            output.println();
        }

        output.flush();
    }

}
