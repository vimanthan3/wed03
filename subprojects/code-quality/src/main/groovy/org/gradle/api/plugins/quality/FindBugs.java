/*
 * Copyright 2016 the original author or authors.
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
package org.gradle.api.plugins.quality;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Incubating;
import org.gradle.api.JavaVersion;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.internal.ClosureBackedAction;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.plugins.quality.internal.FindBugsReportsImpl;
import org.gradle.api.plugins.quality.internal.FindBugsReportsInternal;
import org.gradle.api.plugins.quality.internal.findbugs.FindBugsClasspathValidator;
import org.gradle.api.plugins.quality.internal.findbugs.FindBugsResult;
import org.gradle.api.plugins.quality.internal.findbugs.FindBugsSpec;
import org.gradle.api.plugins.quality.internal.findbugs.FindBugsSpecBuilder;
import org.gradle.api.plugins.quality.internal.findbugs.FindBugsWorkerManager;
import org.gradle.api.reporting.Reporting;
import org.gradle.api.reporting.SingleFileReport;
import org.gradle.api.resources.TextResource;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.VerificationTask;
import org.gradle.internal.logging.ConsoleRenderer;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.process.internal.daemon.WorkerDaemonExpiration;
import org.gradle.process.internal.worker.WorkerProcessFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Analyzes code with <a href="http://findbugs.sourceforge.net">FindBugs</a>. See the <a href="http://findbugs.sourceforge.net/manual/">FindBugs Manual</a> for additional information on configuration
 * options.
 */
@CacheableTask
public class FindBugs extends SourceTask implements VerificationTask, Reporting<FindBugsReports> {

    private FileCollection classes;

    private FileCollection classpath;

    private FileCollection findbugsClasspath;

    private FileCollection pluginClasspath;

    private boolean ignoreFailures;

    private String effort;

    private String reportLevel;

    private String maxHeapSize;

    private Collection<String> visitors = new ArrayList<String>();

    private Collection<String> omitVisitors = new ArrayList<String>();

    private TextResource includeFilterConfig;

    private TextResource excludeFilterConfig;

    private TextResource excludeBugsFilterConfig;

    private Collection<String> extraArgs = new ArrayList<String>();

    @Nested
    private final FindBugsReportsInternal reports;

    public FindBugs() {
        reports = getInstantiator().newInstance(FindBugsReportsImpl.class, this);
    }

    @Inject
    public Instantiator getInstantiator() {
        throw new UnsupportedOperationException();
    }

    @Inject
    public WorkerProcessFactory getWorkerProcessBuilderFactory() {
        throw new UnsupportedOperationException();
    }

    @Inject
    protected WorkerDaemonExpiration getWorkerDaemonExpiration() {
        throw new UnsupportedOperationException();
    }

    /**
     * The reports to be generated by this task.
     *
     * @return The reports container
     */
    public FindBugsReports getReports() {
        return reports;
    }

    /**
     * Configures the reports to be generated by this task.
     *
     * The contained reports can be configured by name and closures. Example:
     *
     * <pre>
     * findbugsTask {
     *   reports {
     *     xml {
     *       destination "build/findbugs.xml"
     *     }
     *   }
     * }
     * </pre>
     *
     * @param closure The configuration
     * @return The reports container
     */
    public FindBugsReports reports(Closure closure) {
        return reports(new ClosureBackedAction<FindBugsReports>(closure));
    }

    /**
     * Configures the reports to be generated by this task.
     *
     * The contained reports can be configured by name and closures. Example:
     *
     * <pre>
     * findbugsTask {
     *   reports {
     *     xml {
     *       destination "build/findbugs.xml"
     *     }
     *   }
     * }
     * </pre>
     *
     *
     * @param configureAction The configuration
     * @return The reports container
     */
    public FindBugsReports reports(Action<? super FindBugsReports> configureAction) {
        configureAction.execute(reports);
        return reports;
    }

    /**
     * The filename of a filter specifying which bugs are reported.
     */
    @Internal
    public File getIncludeFilter() {
        TextResource config = getIncludeFilterConfig();
        return config == null ? null : config.asFile();
    }

    /**
     * The filename of a filter specifying which bugs are reported.
     */
    public void setIncludeFilter(File filter) {
        setIncludeFilterConfig(getProject().getResources().getText().fromFile(filter));
    }

    /**
     * The filename of a filter specifying bugs to exclude from being reported.
     */
    @Internal
    public File getExcludeFilter() {
        TextResource config = getExcludeFilterConfig();
        return config == null ? null : config.asFile();
    }

    /**
     * The filename of a filter specifying bugs to exclude from being reported.
     */
    public void setExcludeFilter(File filter) {
        setExcludeFilterConfig(getProject().getResources().getText().fromFile(filter));
    }

    /**
     * The filename of a filter specifying baseline bugs to exclude from being reported.
     */
    @Internal
    public File getExcludeBugsFilter() {
        TextResource config = getExcludeBugsFilterConfig();
        return config == null ? null : config.asFile();
    }

    /**
     * The filename of a filter specifying baseline bugs to exclude from being reported.
     */
    public void setExcludeBugsFilter(File filter) {
        setExcludeBugsFilterConfig(getProject().getResources().getText().fromFile(filter));
    }

    @TaskAction
    public void run() throws IOException, InterruptedException {
        new FindBugsClasspathValidator(JavaVersion.current()).validateClasspath(
            Iterables.transform(getFindbugsClasspath().getFiles(), new Function<File, String>() {
                @Override
                public String apply(File input) {
                    return input.getName();
                }
            }));
        FindBugsSpec spec = generateSpec();
        FindBugsWorkerManager manager = new FindBugsWorkerManager();

        getLogging().captureStandardOutput(LogLevel.DEBUG);
        getLogging().captureStandardError(LogLevel.DEBUG);

        FindBugsResult result = manager.runWorker(getProject().getProjectDir(), getWorkerDaemonExpiration(), getWorkerProcessBuilderFactory(), getFindbugsClasspath(), spec);
        evaluateResult(result);
    }

    @VisibleForTesting
    FindBugsSpec generateSpec() {
        FindBugsSpecBuilder specBuilder = new FindBugsSpecBuilder(getClasses())
            .withPluginsList(getPluginClasspath())
            .withSources(getSource())
            .withClasspath(getClasspath())
            .withDebugging(getLogger().isDebugEnabled())
            .withEffort(getEffort())
            .withReportLevel(getReportLevel())
            .withMaxHeapSize(getMaxHeapSize())
            .withVisitors(getVisitors())
            .withOmitVisitors(getOmitVisitors())
            .withExcludeFilter(getExcludeFilter())
            .withIncludeFilter(getIncludeFilter())
            .withExcludeBugsFilter(getExcludeBugsFilter())
            .withExtraArgs(getExtraArgs())
            .configureReports(getReports());

        return specBuilder.build();
    }

    @VisibleForTesting
    void evaluateResult(FindBugsResult result) {
        if (result.getException() != null) {
            throw new GradleException("FindBugs encountered an error. Run with --debug to get more information.", result.getException());
        }

        if (result.getErrorCount() > 0) {
            throw new GradleException("FindBugs encountered an error. Run with --debug to get more information.");
        }

        if (result.getBugCount() > 0) {
            String message = "FindBugs rule violations were found.";
            SingleFileReport report = reports.getFirstEnabled();
            if (report != null) {
                String reportUrl = new ConsoleRenderer().asClickableFileUrl(report.getDestination());
                message += " See the report at: " + reportUrl;
            }

            if (getIgnoreFailures()) {
                getLogger().warn(message);
            } else {
                throw new GradleException(message);
            }

        }

    }

    public FindBugs extraArgs(Iterable<String> arguments) {
        for (String argument : arguments) {
            extraArgs.add(argument);
        }

        return this;
    }

    public FindBugs extraArgs(String... arguments) {
        extraArgs.addAll(Arrays.asList(arguments));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PathSensitive(PathSensitivity.RELATIVE)
    public FileTree getSource() {
        return super.getSource();
    }

    /**
     * The classes to be analyzed.
     */
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.RELATIVE)
    @InputFiles
    public FileCollection getClasses() {
        return classes;
    }

    public void setClasses(FileCollection classes) {
        this.classes = classes;
    }

    /**
     * Compile class path for the classes to be analyzed. The classes on this class path are used during analysis but aren't analyzed themselves.
     */
    @Classpath
    public FileCollection getClasspath() {
        return classpath;
    }

    public void setClasspath(FileCollection classpath) {
        this.classpath = classpath;
    }

    /**
     * Class path holding the FindBugs library.
     */
    @Classpath
    public FileCollection getFindbugsClasspath() {
        return findbugsClasspath;
    }

    public void setFindbugsClasspath(FileCollection findbugsClasspath) {
        this.findbugsClasspath = findbugsClasspath;
    }

    /**
     * Class path holding any additional FindBugs plugins.
     */
    @Classpath
    public FileCollection getPluginClasspath() {
        return pluginClasspath;
    }

    public void setPluginClasspath(FileCollection pluginClasspath) {
        this.pluginClasspath = pluginClasspath;
    }

    /**
     * Whether or not to allow the build to continue if there are warnings.
     */
    @Input
    @Override
    public boolean getIgnoreFailures() {
        return ignoreFailures;
    }

    public void setIgnoreFailures(boolean ignoreFailures) {
        this.ignoreFailures = ignoreFailures;
    }

    /**
     * The analysis effort level. The value specified should be one of {@code min}, {@code default}, or {@code max}. Higher levels increase precision and find more bugs at the expense of running time
     * and memory consumption.
     */
    @Input
    @Optional
    public String getEffort() {
        return effort;
    }

    public void setEffort(String effort) {
        this.effort = effort;
    }

    /**
     * The priority threshold for reporting bugs. If set to {@code low}, all bugs are reported. If set to {@code medium} (the default), medium and high priority bugs are reported. If set to {@code
     * high}, only high priority bugs are reported.
     */
    @Input
    @Optional
    public String getReportLevel() {
        return reportLevel;
    }

    public void setReportLevel(String reportLevel) {
        this.reportLevel = reportLevel;
    }

    /**
     * The maximum heap size for the forked findbugs process (ex: '1g').
     */
    @Input
    @Optional
    public String getMaxHeapSize() {
        return maxHeapSize;
    }

    public void setMaxHeapSize(String maxHeapSize) {
        this.maxHeapSize = maxHeapSize;
    }

    /**
     * The bug detectors which should be run. The bug detectors are specified by their class names, without any package qualification. By default, all detectors which are not disabled by default are
     * run.
     */
    @Input
    @Optional
    public Collection<String> getVisitors() {
        return visitors;
    }

    public void setVisitors(Collection<String> visitors) {
        this.visitors = visitors;
    }

    /**
     * Similar to {@code visitors} except that it specifies bug detectors which should not be run. By default, no visitors are omitted.
     */
    @Input
    @Optional
    public Collection<String> getOmitVisitors() {
        return omitVisitors;
    }

    public void setOmitVisitors(Collection<String> omitVisitors) {
        this.omitVisitors = omitVisitors;
    }

    /**
     * A filter specifying which bugs are reported. Replaces the {@code includeFilter} property.
     *
     * @since 2.2
     */
    @Incubating
    @Nested
    @Optional
    public TextResource getIncludeFilterConfig() {
        return includeFilterConfig;
    }

    public void setIncludeFilterConfig(TextResource includeFilterConfig) {
        this.includeFilterConfig = includeFilterConfig;
    }

    /**
     * A filter specifying bugs to exclude from being reported. Replaces the {@code excludeFilter} property.
     *
     * @since 2.2
     */
    @Incubating
    @Nested
    @Optional
    public TextResource getExcludeFilterConfig() {
        return excludeFilterConfig;
    }

    public void setExcludeFilterConfig(TextResource excludeFilterConfig) {
        this.excludeFilterConfig = excludeFilterConfig;
    }

    /**
     * A filter specifying baseline bugs to exclude from being reported.
     */
    @Incubating
    @Nested
    @Optional
    public TextResource getExcludeBugsFilterConfig() {
        return excludeBugsFilterConfig;
    }

    public void setExcludeBugsFilterConfig(TextResource excludeBugsFilterConfig) {
        this.excludeBugsFilterConfig = excludeBugsFilterConfig;
    }

    /**
     * Any additional arguments (not covered here more explicitly like {@code effort}) to be passed along to FindBugs. <p> Extra arguments are passed to FindBugs after the arguments Gradle understands
     * (like {@code effort} but before the list of classes to analyze. This should only be used for arguments that cannot be provided by Gradle directly. Gradle does not try to interpret or validate
     * the arguments before passing them to FindBugs. <p> See the <a href="https://code.google.com/p/findbugs/source/browse/findbugs/src/java/edu/umd/cs/findbugs/TextUICommandLine.java">FindBugs
     * TextUICommandLine source</a> for available options.
     *
     * @since 2.6
     */
    @Input
    @Optional
    public Collection<String> getExtraArgs() {
        return extraArgs;
    }

    public void setExtraArgs(Collection<String> extraArgs) {
        this.extraArgs = extraArgs;
    }

}
