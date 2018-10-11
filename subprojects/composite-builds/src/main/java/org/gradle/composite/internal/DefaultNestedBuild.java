/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.composite.internal;

import org.gradle.api.Action;
import org.gradle.api.Transformer;
import org.gradle.api.artifacts.component.BuildIdentifier;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.BuildDefinition;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.internal.SettingsInternal;
import org.gradle.initialization.GradleLauncher;
import org.gradle.initialization.NestedBuildFactory;
import org.gradle.internal.build.AbstractBuildState;
import org.gradle.internal.build.BuildState;
import org.gradle.internal.build.StandAloneNestedBuild;
import org.gradle.internal.concurrent.Stoppable;
import org.gradle.internal.invocation.BuildController;
import org.gradle.internal.invocation.GradleBuildController;
import org.gradle.util.Path;

class DefaultNestedBuild extends AbstractBuildState implements StandAloneNestedBuild, Stoppable {
    private final Path identityPath;
    private final BuildState owner;
    private final BuildStateListener buildStateListener;
    private final BuildIdentifier buildIdentifier;
    private final BuildDefinition buildDefinition;
    private final GradleLauncher gradleLauncher;
    private SettingsInternal settings;

    DefaultNestedBuild(BuildIdentifier buildIdentifier, Path identityPath, BuildDefinition buildDefinition, BuildState owner, BuildStateListener buildStateListener) {
        this.buildIdentifier = buildIdentifier;
        this.identityPath = identityPath;
        this.buildDefinition = buildDefinition;
        this.owner = owner;
        this.buildStateListener = buildStateListener;
        gradleLauncher = owner.getNestedBuildFactory().nestedInstance(buildDefinition, this);
    }

    @Override
    public BuildIdentifier getBuildIdentifier() {
        return buildIdentifier;
    }

    @Override
    public Path getIdentityPath() {
        return identityPath;
    }

    @Override
    public boolean isImplicitBuild() {
        return true;
    }

    @Override
    public void stop() {
        gradleLauncher.stop();
    }

    @Override
    public <T> T run(Transformer<T, ? super BuildController> buildAction) {
        GradleBuildController buildController = new GradleBuildController(gradleLauncher);
        try {
            gradleLauncher.getGradle().setIdentityPath(getCurrentPrefixForProjectsInChildBuilds());
            final GradleInternal gradle = buildController.getGradle();
            gradle.settingsEvaluated(new Action<Settings>() {
                @Override
                public void execute(Settings settings) {
                    DefaultNestedBuild.this.settings = (SettingsInternal)settings;
                    buildStateListener.projectsKnown(DefaultNestedBuild.this);
                }
            });
            return buildAction.transform(buildController);
        } finally {
            buildController.stop();
        }
    }

    @Override
    public SettingsInternal getLoadedSettings() {
        if (settings == null) {
            throw new IllegalStateException("Settings not loaded yet.");
        }
        return settings;
    }

    @Override
    public NestedBuildFactory getNestedBuildFactory() {
        return gradleLauncher.getGradle().getServices().get(NestedBuildFactory.class);
    }

    @Override
    public Path getCurrentPrefixForProjectsInChildBuilds() {
        return owner.getCurrentPrefixForProjectsInChildBuilds().child(buildDefinition.getName());
    }

    @Override
    public Path getIdentityPathForProject(Path projectPath) {
        return getLoadedSettings().getGradle().getIdentityPath();
    }
}
