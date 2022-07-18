/*
 * Copyright 2020 the original author or authors.
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

package org.gradle.jvm.toolchain.internal;

import org.gradle.api.file.RegularFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.WorkResult;
import org.gradle.internal.operations.BuildOperationProgressEventEmitter;
import org.gradle.jvm.toolchain.JavaCompiler;
import org.gradle.jvm.toolchain.JavaInstallationMetadata;
import org.gradle.jvm.toolchain.internal.DefaultJavaToolchainUsageProgressDetails.JavaTool;
import org.gradle.language.base.internal.compile.CompileSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultToolchainJavaCompiler implements JavaCompiler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultToolchainJavaCompiler.class);

    private final JavaToolchain javaToolchain;
    private final JavaCompilerFactory compilerFactory;
    private final BuildOperationProgressEventEmitter eventEmitter;

    public DefaultToolchainJavaCompiler(JavaToolchain javaToolchain, JavaCompilerFactory compilerFactory, BuildOperationProgressEventEmitter eventEmitter) {
        this.javaToolchain = javaToolchain;
        this.compilerFactory = compilerFactory;
        this.eventEmitter = eventEmitter;
    }

    @Override
    @Nested
    public JavaInstallationMetadata getMetadata() {
        emitToolchainEvent();
        return javaToolchain;
    }

    @Override
    @Internal
    public RegularFile getExecutablePath() {
        return javaToolchain.findExecutable("javac");
    }

    @SuppressWarnings("unchecked")
    public <T extends CompileSpec> WorkResult execute(T spec) {
        LOGGER.info("Compiling with toolchain '{}'.", javaToolchain.getDisplayName());
        emitToolchainEvent();
        final Class<T> specType = (Class<T>) spec.getClass();
        return compilerFactory.create(specType).execute(spec);
    }

    private void emitToolchainEvent() {
        eventEmitter.emitNowForCurrent(new DefaultJavaToolchainUsageProgressDetails(JavaTool.COMPILER, javaToolchain));
    }

}
