package io.spring.gradle.dependencymanagement.maven;

import groovy.util.Node;
import io.spring.gradle.dependencymanagement.DependencyManagement;
import io.spring.gradle.dependencymanagement.DependencyManagementExtension;
import io.spring.gradle.dependencymanagement.DependencyManagementExtension.PomCustomizationConfiguration;
import io.spring.gradle.dependencymanagement.ManagedDependency;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.Dependency;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.Exclusion;
import org.gradle.api.Action;
import org.gradle.api.XmlProvider;

import java.util.List;
import java.util.Map;

/**
 * Configures the dependency management in a Maven pom produced as part of a Gradle build
 *
 * @author Andy Wilkinson
 */
public class PomDependencyManagementConfigurer implements Action<XmlProvider> {

    private final DependencyManagement dependencyManagement;
    private DependencyManagementExtension.PomCustomizationConfiguration configuration;

    public PomDependencyManagementConfigurer(DependencyManagement dependencyManagement,
            PomCustomizationConfiguration configuration) {
        this.dependencyManagement = dependencyManagement;
        this.configuration = configuration;
    }

    @Override
    public void execute(XmlProvider xmlProvider) {
        configurePom(xmlProvider.asNode());
    }

    /**
     * Configures the dependency management of the given {@code pom}
     *
     * @param pom
     *         the pom to configure
     */
    public void configurePom(Node pom) {
        if (configuration.isEnabled()) {
            doConfigurePom(pom);
        }

    }

    private void doConfigurePom(Node pom) {
        Node dependencyManagement = findChild(pom, "dependencyManagement");
        if (dependencyManagement == null) {
            dependencyManagement = pom.appendNode("dependencyManagement");
        }

        Node dependencies = findChild(dependencyManagement, "dependencies");
        if (dependencies == null) {
            dependencies = dependencyManagement.appendNode("dependencies");
        }

        configureBomImports(dependencies);
        configureDependencies(dependencies);
    }

    private Node findChild(Node node, String name) {
        for (Object childObject : node.children()) {
            if ((childObject instanceof Node) && ((Node)childObject).name().equals(name)) {
                return (Node)childObject;
            }

        }

        return null;
    }

    private void configureBomImports(Node dependencies) {
        Map<String, List<Dependency>> importedBoms = this.dependencyManagement.getImportedBoms();
        for (Map.Entry<String, List<Dependency>> entry : importedBoms.entrySet()) {
            if (configuration.getImportedBomAction()
                    .equals(PomCustomizationConfiguration.ImportedBomAction.IMPORT)) {
                addImport(dependencies, entry.getKey());
            }
            else {
                for (Dependency dependency : entry.getValue()) {
                    addDependency(dependencies, dependency);
                }

            }

        }

    }

    private void addImport(Node dependencies, String bomCoordinates) {
        String[] splitCoordinates = bomCoordinates.split(":");
        Node dependency = dependencies.appendNode("dependency");
        dependency.appendNode("groupId", splitCoordinates[0]);
        dependency.appendNode("artifactId", splitCoordinates[1]);
        dependency.appendNode("version", splitCoordinates[2]);
        dependency.appendNode("scope", "import");
        dependency.appendNode("type", "pom");
    }

    private void addDependency(Node dependencies, Dependency dependencyToAdd) {
        Node dependency = dependencies.appendNode("dependency");
        dependency.appendNode("groupId", dependencyToAdd.getGroupId());
        dependency.appendNode("artifactId", dependencyToAdd.getArtifactId());
        dependency.appendNode("version", dependencyToAdd.getVersion());
        if (!dependencyToAdd.getType().equals("jar")) {
            dependency.appendNode("type", dependencyToAdd.getType());
        }
        if (dependencyToAdd.getClassifier() != null) {
            dependency.appendNode("classifier", dependencyToAdd.getClassifier());
        }

        if (dependencyToAdd.getScope() != null) {
            dependency.appendNode("scope", dependencyToAdd.getScope());
        }

        if (dependencyToAdd.getExclusions() != null) {
            Node exclusions = dependency.appendNode("exclusions");
            for (Exclusion exclusion : dependencyToAdd.getExclusions()) {
                addExclusion(exclusions, exclusion);
            }

        }

    }

    private void addExclusion(Node exclusions, Exclusion exclusionToAdd) {
        Node exclusion = exclusions.appendNode("exclusion");
        exclusion.appendNode("groupId", exclusionToAdd.getGroupId());
        exclusion.appendNode("artifactId", exclusionToAdd.getArtifactId());
    }

    private void configureDependencies(Node dependencies) {
        for (ManagedDependency managedDependency : this.dependencyManagement
                .getExplicitlyManagedDependencies()) {
            Node dependency = dependencies.appendNode("dependency");
            dependency.appendNode("groupId", managedDependency.getGroupId());
            dependency.appendNode("artifactId", managedDependency.getArtifactId());
            dependency.appendNode("version", managedDependency.getVersion());
            if (!managedDependency.getExclusions().isEmpty()) {
                Node exclusionsNode = dependency.appendNode("exclusions");
                for (String exclusion : managedDependency.getExclusions()) {
                    String[] exclusionComponents = exclusion.split(":");
                    Node exclusionNode = exclusionsNode.appendNode("exclusion");
                    exclusionNode.appendNode("groupId", exclusionComponents[0]);
                    exclusionNode.appendNode("artifactId", exclusionComponents[1]);
                }

            }

        }

    }

    public PomCustomizationConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(PomCustomizationConfiguration configuration) {
        this.configuration = configuration;
    }

}
