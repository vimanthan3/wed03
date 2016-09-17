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

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import org.gradle.api.Action;
import org.gradle.api.Incubating;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.internal.ClosureBackedAction;
import org.gradle.api.internal.project.IsolatedAntBuilder;
import org.gradle.api.plugins.quality.internal.CheckstyleInvoker;
import org.gradle.api.plugins.quality.internal.CheckstyleReportsImpl;
import org.gradle.api.reporting.Reporting;
import org.gradle.api.resources.TextResource;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Console;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.VerificationTask;
import org.gradle.internal.reflect.Instantiator;

import javax.inject.Inject;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runs Checkstyle against some source files.
 */
@CacheableTask
public class Checkstyle extends SourceTask implements VerificationTask, Reporting<CheckstyleReports> {

    private FileCollection checkstyleClasspath;
    private FileCollection classpath;
    private TextResource config;
    private Map<String, Object> configProperties = new LinkedHashMap<String, Object>();
    private final CheckstyleReports reports;
    private boolean ignoreFailures;
    private boolean showViolations = true;


    /**
     * The Checkstyle configuration file to use.
     */
    @Internal
    public File getConfigFile() {
        return getConfig() == null ? null : getConfig().asFile();
    }

    /**
     * The Checkstyle configuration file to use.
     */
    public void setConfigFile(File configFile) {
        setConfig(getProject().getResources().getText().fromFile(configFile));
    }

    public Checkstyle() {
        reports = getInstantiator().newInstance(CheckstyleReportsImpl.class, this);
    }

    @Inject
    public Instantiator getInstantiator() {
        throw new UnsupportedOperationException();
    }

    @Inject
    public IsolatedAntBuilder getAntBuilder() {
        throw new UnsupportedOperationException();
    }

    /**
     * Configures the reports to be generated by this task.
     *
     * The contained reports can be configured by name and closures. Example:
     *
     * <pre>
     * checkstyleTask {
     *   reports {
     *     html {
     *       destination "build/codenarc.html"
     *     }
     *   }
     * }
     * </pre>
     *
     * @param closure The configuration
     * @return The reports container
     */
    public CheckstyleReports reports(@DelegatesTo(value=CheckstyleReports.class, strategy = Closure.DELEGATE_FIRST) Closure closure) {
        return reports(new ClosureBackedAction<CheckstyleReports>(closure));
    }

    /**
     * Configures the reports to be generated by this task.
     *
     * The contained reports can be configured by name and closures. Example:
     *
     * <pre>
     * checkstyleTask {
     *   reports {
     *     html {
     *       destination "build/codenarc.html"
     *     }
     *   }
     * }
     * </pre>
     *
     * @since 3.0
     * @param configureAction The configuration
     * @return The reports container
     */
    public CheckstyleReports reports(Action<? super CheckstyleReports> configureAction) {
        configureAction.execute(reports);
        return reports;
    }

    @TaskAction
    public void run() {
        CheckstyleInvoker.invoke(this);
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
     * The class path containing the Checkstyle library to be used.
     */
    @Classpath
    public FileCollection getCheckstyleClasspath() {
        return checkstyleClasspath;
    }

    public void setCheckstyleClasspath(FileCollection checkstyleClasspath) {
        this.checkstyleClasspath = checkstyleClasspath;
    }

    /**
     * The class path containing the compiled classes for the source files to be analyzed.
     */
    @Classpath
    public FileCollection getClasspath() {
        return classpath;
    }

    public void setClasspath(FileCollection classpath) {
        this.classpath = classpath;
    }

    /**
     * The Checkstyle configuration to use. Replaces the {@code configFile} property.
     *
     * @since 2.2
     */
    @Incubating
    @Nested
    public TextResource getConfig() {
        return config;
    }

    public void setConfig(TextResource config) {
        this.config = config;
    }

    /**
     * The properties available for use in the configuration file. These are substituted into the configuration file.
     */
    @Input
    @Optional
    public Map<String, Object> getConfigProperties() {
        return configProperties;
    }

    public void setConfigProperties(Map<String, Object> configProperties) {
        this.configProperties = configProperties;
    }

    /**
     * The reports to be generated by this task.
     */
    @Nested
    public final CheckstyleReports getReports() {
        return reports;
    }

    /**
     * Whether or not this task will ignore failures and continue running the build.
     *
     * @return true if failures should be ignored
     */
    @Input
    public boolean getIgnoreFailures() {
        return ignoreFailures;
    }

    /**
     * Whether or not this task will ignore failures and continue running the build.
     *
     * @return true if failures should be ignored
     */
    public boolean isIgnoreFailures() {
        return ignoreFailures;
    }

    public void setIgnoreFailures(boolean ignoreFailures) {
        this.ignoreFailures = ignoreFailures;
    }

    /**
     * Whether or not rule violations are to be displayed on the console.
     *
     * @return true if violations should be displayed on console
     */
    @Console
    public boolean isShowViolations() {
        return showViolations;
    }

    public void setShowViolations(boolean showViolations) {
        this.showViolations = showViolations;
    }

}
