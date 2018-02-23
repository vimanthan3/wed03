/*
 * Copyright 2009 the original author or authors.
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

package org.gradle.external.javadoc;

import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Console;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.process.ExecSpec;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Provides the core Javadoc options.
 */
public interface MinimalJavadocOptions {
    @Nullable @Input
    String getOverview();

    void setOverview(@Nullable String overview);

    MinimalJavadocOptions overview(String overview);

    @Nullable @Input
    JavadocMemberLevel getMemberLevel();

    void setMemberLevel(@Nullable JavadocMemberLevel memberLevel);

    MinimalJavadocOptions showFromPublic();

    MinimalJavadocOptions showFromProtected();

    MinimalJavadocOptions showFromPackage();

    MinimalJavadocOptions showFromPrivate();

    MinimalJavadocOptions showAll();

    @Nullable @Input
    String getDoclet();

    void setDoclet(@Nullable String docletClass);

    MinimalJavadocOptions doclet(String docletClass);

    @Classpath
    List<File> getDocletpath();

    void setDocletpath(List<File> docletpath);

    MinimalJavadocOptions docletpath(File ... docletpath);

    @Nullable @Input
    String getSource();

    void setSource(@Nullable String source);

    MinimalJavadocOptions source(String source);

    @Internal
    List<File> getClasspath();

    void setClasspath(List<File> classpath);

    MinimalJavadocOptions classpath(List<File> classpath);

    MinimalJavadocOptions classpath(File ... classpath);

    @Classpath
    List<File> getBootClasspath();

    void setBootClasspath(List<File> bootClasspath);

    MinimalJavadocOptions bootClasspath(File ... bootClasspath);

    @Nullable @PathSensitive(PathSensitivity.RELATIVE) @InputFiles
    List<File> getExtDirs();

    void setExtDirs(@Nullable List<File> extDirs);

    MinimalJavadocOptions extDirs(File ... extDirs);

    @Console
    JavadocOutputLevel getOutputLevel();

    void setOutputLevel(JavadocOutputLevel outputLevel);

    MinimalJavadocOptions verbose();

    @Internal
    boolean isVerbose();

    MinimalJavadocOptions quiet();

    @Input
    boolean isBreakIterator();

    void setBreakIterator(boolean breakIterator);

    MinimalJavadocOptions breakIterator(boolean breakIterator);

    MinimalJavadocOptions breakIterator();

    @Nullable @Input
    String getLocale();

    void setLocale(@Nullable String locale);

    MinimalJavadocOptions locale(String locale);

    @Nullable @Input
    String getEncoding();

    void setEncoding(@Nullable String encoding);

    MinimalJavadocOptions encoding(String encoding);

    @Nullable @Input
    List<String> getJFlags();

    void setJFlags(@Nullable List<String> jFlags);

    MinimalJavadocOptions jFlags(String ... jFlags);

    @Nullable @PathSensitive(PathSensitivity.NONE) @InputFiles
    List<File> getOptionFiles();

    void setOptionFiles(@Nullable List<File> optionFiles);

    MinimalJavadocOptions optionFiles(File ... argumentFiles);

    @Internal
    File getDestinationDirectory();

    void setDestinationDirectory(@Nullable File directory);

    MinimalJavadocOptions destinationDirectory(File directory);

    @Nullable @Input
    String getWindowTitle();

    void setWindowTitle(@Nullable String windowTitle);

    StandardJavadocDocletOptions windowTitle(String windowTitle);

    @Nullable @Input
    String getHeader();

    void setHeader(@Nullable String header);

    StandardJavadocDocletOptions header(String header);

    void write(File outputFile) throws IOException;

    @Internal
    List<String> getSourceNames();

    void setSourceNames(@Nullable List<String> sourceNames);

    MinimalJavadocOptions sourceNames(String ... sourceNames);

    void contributeCommandLineOptions(ExecSpec execHandleBuilder);
}
