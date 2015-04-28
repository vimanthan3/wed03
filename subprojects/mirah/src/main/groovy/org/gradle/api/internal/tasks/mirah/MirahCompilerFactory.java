/*
 * Copyright 2012 the original author or authors.
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

package org.gradle.api.internal.tasks.mirah;

import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.tasks.compile.JavaCompileSpec;
import org.gradle.api.internal.tasks.compile.JavaCompilerFactory;
import org.gradle.api.internal.tasks.compile.daemon.CompilerDaemonFactory;
import org.gradle.api.tasks.mirah.MirahCompileOptions;
import org.gradle.language.base.internal.compile.Compiler;
import org.gradle.language.base.internal.compile.CompilerFactory;

import java.io.File;
import java.util.Set;

public class MirahCompilerFactory implements CompilerFactory<MirahCompileSpec> {
    private final CompilerDaemonFactory compilerDaemonFactory;
    private FileCollection mirahClasspath;
    private final File rootProjectDirectory;

    public MirahCompilerFactory(File rootProjectDirectory, CompilerDaemonFactory compilerDaemonFactory, FileCollection mirahClasspath) {
        this.rootProjectDirectory = rootProjectDirectory;
        this.compilerDaemonFactory = compilerDaemonFactory;
        this.mirahClasspath = mirahClasspath;
    }

    @SuppressWarnings("unchecked")
    public Compiler<MirahCompileSpec> newCompiler(MirahCompileSpec spec) {
        MirahCompileOptions mirahOptions = (MirahCompileOptions) spec.getMirahCompileOptions();
        Set<File> mirahClasspathFiles = mirahClasspath.getFiles();
        Compiler<MirahCompileSpec> mirahCompiler = new DaemonMirahCompiler<MirahCompileSpec>(rootProjectDirectory, new MirahCompiler(mirahClasspathFiles), compilerDaemonFactory, mirahClasspathFiles);
        return new NormalizingMirahCompiler(mirahCompiler);
    }
}
