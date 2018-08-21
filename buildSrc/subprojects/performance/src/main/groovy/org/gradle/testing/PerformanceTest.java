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

package org.gradle.testing;

import org.gradle.api.internal.tasks.options.Option;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.gradlebuild.test.integrationtests.DistributionTest;
import org.gradle.api.specs.Spec;
import org.gradle.api.Task;

import org.gradle.api.tasks.CacheableTask;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

import org.gradle.process.CommandLineArgumentProvider;

/**
 * A test that checks execution time and memory consumption.
 */
@CacheableTask
public class PerformanceTest extends DistributionTest {
    protected PerformanceTestJvmArgumentsProvider argumentsProvider = new PerformanceTestJvmArgumentsProvider();

    private File debugArtifactsDirectory = new File(getProject().getBuildDir(), getName());

    public PerformanceTest() {
        getJvmArgumentProviders().add(argumentsProvider);

        getOutputs().doNotCacheIf("last and nightly are not concrete version so can't be cached", new Spec<Task>() {
            @Override
            public boolean isSatisfiedBy(Task task) {
                List<String> baseLineList = new ArrayList<>();
                if (argumentsProvider.baselines != null) {
                    for (String baseline : argumentsProvider.baselines.split(",")) {
                        baseLineList.add(baseline.trim());
                    }
                }

                return baseLineList.contains("last") || baseLineList.contains("nightly");
            }
        });
    }

    @Nested
    public org.gradle.testing.PerformanceTest.PerformanceTestJvmArgumentsProvider getArgumentsProvider() {
        return argumentsProvider;
    }

    @OutputDirectory
    public File getDebugArtifactsDirectory() {
        return debugArtifactsDirectory;
    }

    @Option(option = "scenarios", description = "A semicolon-separated list of performance test scenario ids to run.")
    public void setScenarios(String scenarios) {
        argumentsProvider.scenarios = scenarios;
    }

    @Option(option = "baselines", description = "A comma or semicolon separated list of Gradle versions to be used as baselines for comparing.")
    public void setBaselines(@Nullable String baselines) {
        argumentsProvider.baselines = baselines;
    }

    @Option(option = "warmups", description = "Number of warmups before measurements")
    public void setWarmups(@Nullable String warmups) {
        argumentsProvider.warmups = warmups;
    }

    @Option(option = "runs", description = "Number of iterations of measurements")
    public void setRuns(@Nullable String runs) {
        argumentsProvider.runs = runs;
    }

    @Option(option = "flamegraphs", description = "If set to 'true', activates flamegraphs and stores them into the 'flames' directory name under the debug artifacts directory.")
    public void setFlamegraphs(String flamegraphs) {
        argumentsProvider.flamegraphs = "true".equals(flamegraphs);
    }

    @Option(option = "checks", description = "Tells which regressions to check. One of [none, speed, all]")
    public void setChecks(@Nullable String checks) {
        argumentsProvider.checks = checks;
    }

    @Option(option = "channel", description = "Channel to use when running the performance test. By default, 'commits'.")
    public void setChannel(@Nullable String channel) {
        argumentsProvider.channel = channel;
    }

    public void setResultStoreClass(String resultStoreClass) {
        argumentsProvider.resultStoreClass = resultStoreClass;
    }

    @Internal
    public String getBaselines() {
        return argumentsProvider.baselines;
    }

    @Internal
    public String getWarmups() {
        return argumentsProvider.warmups;
    }

    @Internal
    public String getRuns() {
        return argumentsProvider.runs;
    }

    @Internal
    public String getChecks() {
        return argumentsProvider.checks;
    }

    @Internal
    public String getChannel() {
        return argumentsProvider.channel;
    }

    public void setDebugArtifactsDirectory(File debugArtifactsDirectory) {
        this.debugArtifactsDirectory = debugArtifactsDirectory;
    }

    private class PerformanceTestJvmArgumentsProvider implements CommandLineArgumentProvider {
        private String scenarios;
        private String baselines;
        private String warmups;
        private String runs;
        private String checks;
        private String channel;
        private String resultStoreClass = "org.gradle.performance.results.AllResultsStore";
        private boolean flamegraphs;

        @Input
        @Nullable
        @Optional
        public String getScenarios() {
            return scenarios;
        }

        @Input
        @Nullable
        @Optional
        public String getBaselines() {
            return baselines;
        }

        @Input
        @Nullable
        @Optional
        public String getWarmups() {
            return warmups;
        }

        @Input
        @Nullable
        @Optional
        public String getRuns() {
            return runs;
        }

        @Input
        @Nullable
        @Optional
        public String getChecks() {
            return checks;
        }

        @Input
        @Nullable
        @Optional
        public String getChannel() {
            return channel;
        }

        @Input
        @Nullable
        @Optional
        public String getResultStoreClass() {
            return resultStoreClass;
        }

        @Input
        @Nullable
        @Optional
        public boolean isFlamegraphs() {
            return flamegraphs;
        }

        @Override
        public Iterable<String> asArguments() {
            List<String> result = new ArrayList<>();
            addSystemPropertyIfExist(result, "org.gradle.performance.scenarios", scenarios);
            addSystemPropertyIfExist(result, "org.gradle.performance.baselines", baselines);
            addSystemPropertyIfExist(result, "org.gradle.performance.execution.warmups", warmups);
            addSystemPropertyIfExist(result, "org.gradle.performance.execution.runs", runs);
            addSystemPropertyIfExist(result, "org.gradle.performance.execution.checks", checks);
            addSystemPropertyIfExist(result, "org.gradle.performance.execution.channel", channel);

            File artifactsDirectory = new File(getDebugArtifactsDirectory(), "flames");
            addSystemPropertyIfExist(result, "org.gradle.performance.flameGraphTargetDir", getProject().getRootDir().toPath().relativize(artifactsDirectory.toPath()));

            return result;
        }

        private void addSystemPropertyIfExist(List<String> result, String propertyName, Object propertyValue) {
            if (propertyValue != null) {
                result.add("-D" + propertyName + "=" + propertyValue.toString());
            }
        }
    }
}
