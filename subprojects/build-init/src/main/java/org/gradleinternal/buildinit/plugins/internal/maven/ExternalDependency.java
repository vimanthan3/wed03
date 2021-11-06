/*
 * Copyright 2021 the original author or authors.
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

package org.gradleinternal.buildinit.plugins.internal.maven;

import org.gradle.buildinit.plugins.internal.DependencyExclusion;

import java.util.List;

public class ExternalDependency extends Dependency {
    private final String group;
    private final String module;
    private final String version;
    private final String classifier;
    private final List<DependencyExclusion> exclusions;

    public ExternalDependency(String configuration, String group, String module, String version, String classifier, List<DependencyExclusion> exclusions) {
        super(configuration);
        this.group = group;
        this.module = module;
        this.version = version;
        this.classifier = classifier;
        this.exclusions = exclusions;
    }

    public String getGroupId() {
        return group;
    }

    public String getModule() {
        return module;
    }

    public String getVersion() {
        return version;
    }

    public String getClassifier() {
        return classifier;
    }

    public List<DependencyExclusion> getExclusions() {
        return exclusions;
    }
}
