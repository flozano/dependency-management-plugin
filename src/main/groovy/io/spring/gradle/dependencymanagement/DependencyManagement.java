/*
 * Copyright 2014-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.spring.gradle.dependencymanagement;

import io.spring.gradle.dependencymanagement.exclusions.Exclusions;
import io.spring.gradle.dependencymanagement.maven.EffectiveModelBuilder;
import io.spring.gradle.dependencymanagement.maven.ModelExclusionCollector;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.Model;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Encapsulates dependency management information for a particular configuration in a Gradle project
 *
 * @author Andy Wilkinson
 */
public class DependencyManagement {

    private final Logger log = LoggerFactory.getLogger(DependencyManagement.class);

    private final Project project;

    private final Configuration configuration;

    private final Configuration targetConfiguration;

    private final EffectiveModelBuilder effectiveModelBuilder;

    private boolean resolved;

    private Map<String, String> versions = new HashMap<String, String>();

    private Map<String, String> explicitVersions = new HashMap<String, String>();

    private Exclusions explicitExclusions = new Exclusions();

    private Exclusions allExclusions = new Exclusions();

    private Map<String, List<io.spring.gradle.dependencymanagement.org.apache.maven.model.Dependency>> bomDependencyManagement = new LinkedHashMap<String, List<io.spring.gradle.dependencymanagement.org.apache.maven.model.Dependency>>();

    private Properties bomProperties = new Properties();

    private List<ImportedBom> importedBoms = new ArrayList<ImportedBom>();

    DependencyManagement(Project project, Configuration dependencyManagementConfiguration,
            EffectiveModelBuilder effectiveModelBuilder) {
        this(project, null, dependencyManagementConfiguration, effectiveModelBuilder);
    }

    DependencyManagement(Project project, Configuration targetConfiguration, Configuration
            dependencyManagementConfiguration, EffectiveModelBuilder effectiveModelBuilder) {
        this.project = project;
        this.configuration = dependencyManagementConfiguration;
        this.targetConfiguration = targetConfiguration;
        this.effectiveModelBuilder = effectiveModelBuilder;
    }

    void importBom(String coordinates, Map<String, String> properties) {
        importedBoms.add(new ImportedBom(project.getDependencies().create(coordinates + "@pom"),
                properties));
    }

    public Map getImportedBoms() {
        resolveIfNecessary();
        return bomDependencyManagement;
    }

    Properties getImportedProperties() {
        resolveIfNecessary();
        return bomProperties;
    }

    void addManagedVersion(String group, String name, String version) {
        versions.put(createKey(group, name), version);
    }

    void addImplicitManagedVersion(String group, String name, String version) {
        addManagedVersion(group, name, version);
    }

    void addExplicitManagedVersion(String group, String name, String version, List<String>
            exclusions) {
        String key = createKey(group, name);
        explicitVersions.put(key, version);
        explicitExclusions.add(key, exclusions);
        allExclusions.add(key, exclusions);
        addManagedVersion(group, name, version);
    }

    String getManagedVersion(String group, String name) {
        resolveIfNecessary();
        return versions.get(createKey(group, name));
    }

    Map<String, String> getManagedVersions() {
        resolveIfNecessary();
        return new HashMap(versions);
    }

    public List<ManagedDependency> getExplicitlyManagedDependencies() {
        List<ManagedDependency> managedDependencies = new ArrayList<ManagedDependency>();
        for (Map.Entry<String, String> entry: explicitVersions.entrySet()) {
            String[] components = entry.getKey().split(":");
            managedDependencies.add(new ManagedDependency(components[0], components[1],
                    entry.getValue(), explicitExclusions.exclusionsForDependency(entry.getKey())));
        }
        return managedDependencies;
    }

    private String createKey(String group, String name) {
        return group + ":" + name;
    }

    Exclusions getExclusions() {
        resolveIfNecessary();
        return allExclusions;
    }

    private void resolveIfNecessary() {
        if (!resolved) {
            try {
                resolved = true;
                resolve();
            } catch (Exception ex) {
                throw new GradleException("Failed to resolve imported Maven boms: " +
                        getRootCause(ex).getMessage(), ex);
            }
        }
    }

    private Throwable getRootCause(Exception ex) {
        Throwable candidate = ex;
        while(candidate.getCause() != null) {
            candidate = candidate.getCause();
        }
        return candidate;
    }

    private void resolve() {
        if (targetConfiguration != null) {
            log.info("Resolving dependency management for configuration '{}' of project '{}'",
                    targetConfiguration.getName(), project.getName());
        }
        else {
            log.info("Resolving global dependency management for project '{}'", project.getName());
        }
        Map<String, String> existingVersions = new HashMap<String, String>();
        existingVersions.putAll(versions);

        log.debug("Preserving existing versions: {}", existingVersions);

        for (ImportedBom importedBom: this.importedBoms) {
            configuration.getDependencies().add(importedBom.dependency);
        }

        Map<String, File> artifacts = new HashMap<String, File>();
        for (ResolvedArtifact resolvedArtifact: configuration.getResolvedConfiguration()
                .getResolvedArtifacts()) {
            ModuleVersionIdentifier id = resolvedArtifact.getModuleVersion().getId();
            artifacts.put(createKey(id.getGroup(), id.getName()), resolvedArtifact.getFile());
        }

        for (ImportedBom importedBom: this.importedBoms) {
            File file = artifacts.get(createKey(importedBom.dependency.getGroup(), importedBom
                    .dependency.getName()));
            log.debug("Processing '{}'", file);
            Model effectiveModel = this.effectiveModelBuilder.buildModel(file,
                    importedBom.bomProperties);
            if (effectiveModel.getDependencyManagement() != null) {
                List<io.spring.gradle.dependencymanagement.org.apache.maven.model.Dependency> dependencies = effectiveModel.getDependencyManagement()
                        .getDependencies();
                if (dependencies != null) {
                    for (io.spring.gradle.dependencymanagement.org.apache.maven.model.Dependency dependency : dependencies) {
                        this.versions.
                                put(createKey(dependency.getGroupId(), dependency.getArtifactId()),
                                        dependency.getVersion());
                    }
                    String bomCoordinates = effectiveModel.getGroupId() + ":" + effectiveModel
                            .getArtifactId() + ":" + effectiveModel.getVersion();
                    this.bomDependencyManagement.put(bomCoordinates, dependencies);
                    allExclusions.addAll(new ModelExclusionCollector().collectExclusions
                            (effectiveModel));
                }
            }
            if (effectiveModel.getProperties() != null) {
                bomProperties.putAll(effectiveModel.getProperties());
            }
        }

        this.versions.putAll(existingVersions);
    }

    private class ImportedBom {

        private final Dependency dependency;

        private final Map<String, String> bomProperties;

        private ImportedBom(Dependency dependency, Map<String, String> properties) {
            this.dependency = dependency;
            this.bomProperties = properties;
        }

    }

}
