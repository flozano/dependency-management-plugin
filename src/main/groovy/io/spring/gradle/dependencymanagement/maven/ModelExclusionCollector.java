package io.spring.gradle.dependencymanagement.maven;

import io.spring.gradle.dependencymanagement.exclusions.Exclusions;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.Dependency;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.Exclusion;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.Model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Collects exclusions from a Maven {@link Model}
 *
 * @author Andy Wilkinson
 */
public class ModelExclusionCollector {

    private static final Set<String> IGNORED_SCOPES = Collections
            .unmodifiableSet(new HashSet<String>(Arrays.asList("provided", "test")));

    /**
     * Collects the exclusions from the given {@code model}. Exclusions are collected from
     * dependencies in the model's dependency management and its main dependencies
     *
     * @param model
     *         The model to collect the exclusions from
     * @return The collected exclusions
     */
    public Exclusions collectExclusions(Model model) {
        Exclusions exclusions = new Exclusions();
        List<Dependency> dependencies = getManagedDependencies(model);
        dependencies.addAll(getDependencies(model));
        for (Dependency dependency : dependencies) {
            if (dependency.getExclusions() != null && !dependency.isOptional() && !IGNORED_SCOPES
                    .contains(dependency.getScope())) {
                String dependencyId = dependency.getGroupId() + ":" + dependency.getArtifactId();
                Set<String> dependencyExclusions = new HashSet<String>();
                for (Exclusion exlusion : dependency.getExclusions()) {
                    dependencyExclusions
                            .add(exlusion.getGroupId() + ":" + exlusion.getArtifactId());
                }

                exclusions.add(dependencyId, dependencyExclusions);
            }

        }
        return exclusions;
    }

    private List<Dependency> getManagedDependencies(Model model) {
        List<Dependency> managedDependencies = new ArrayList<Dependency>();
        if (model != null && model.getDependencyManagement() != null && model
                .getDependencyManagement().getDependencies() != null) {
            managedDependencies.addAll(model.getDependencyManagement().getDependencies());
        }
        return managedDependencies;
    }

    private List<Dependency> getDependencies(Model model) {
        List<Dependency> dependencies = new ArrayList<Dependency>();
        if (model != null && model.getDependencies() != null) {
            dependencies.addAll(model.getDependencies());
        }
        return dependencies;
    }

}
