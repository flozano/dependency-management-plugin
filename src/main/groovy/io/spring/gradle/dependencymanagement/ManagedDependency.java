package io.spring.gradle.dependencymanagement;

import java.util.Collections;
import java.util.Set;

/**
 * A managed dependency
 *
 * @author Andy Wilkinson
 */
public final class ManagedDependency {

    private final String groupId;

    private final String artifactId;

    private final String version;

    private final Set<String> exclusions;

    ManagedDependency(String groupId, String artifactId, String version,
            Set<String> exclusions) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.exclusions = exclusions == null ? Collections.<String>emptySet(): exclusions;
    }

    /**
     * The group id of the dependency
     * @return the group id
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * The artifact id of the dependency
     *
     * @return the artifact id
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * The version of the dependency
     *
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * The exclusions of the dependency
     *
     * @return the exclusions
     */
    public Set<String> getExclusions() {
        return exclusions;
    }

}
