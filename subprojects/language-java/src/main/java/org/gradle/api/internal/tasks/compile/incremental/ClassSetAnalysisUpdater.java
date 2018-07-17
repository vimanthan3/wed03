/*
 * Copyright 2013 the original author or authors.
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

package org.gradle.api.internal.tasks.compile.incremental;

import com.google.common.collect.Sets;
import org.gradle.api.internal.file.FileOperations;
import org.gradle.api.internal.tasks.compile.JavaCompileSpec;
import org.gradle.api.internal.tasks.compile.JdkJavaCompilerResult;
import org.gradle.api.internal.tasks.compile.incremental.analyzer.ClassDependenciesAnalyzer;
import org.gradle.api.internal.tasks.compile.incremental.analyzer.CompilationResultAnalyzer;
import org.gradle.api.internal.tasks.compile.incremental.deps.ClassSetAnalysisData;
import org.gradle.api.internal.tasks.compile.incremental.processing.AnnotationProcessingResult;
import org.gradle.api.internal.tasks.compile.processing.AnnotationProcessorDeclaration;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.WorkResult;
import org.gradle.cache.internal.Stash;
import org.gradle.internal.hash.FileHasher;
import org.gradle.internal.time.Time;
import org.gradle.internal.time.Timer;

import java.io.File;
import java.util.Set;

public class ClassSetAnalysisUpdater {

    private final static Logger LOG = Logging.getLogger(ClassSetAnalysisUpdater.class);

    private final Stash<ClassSetAnalysisData> stash;
    private final FileOperations fileOperations;
    private ClassDependenciesAnalyzer analyzer;
    private final FileHasher fileHasher;

    ClassSetAnalysisUpdater(Stash<ClassSetAnalysisData> stash, FileOperations fileOperations, ClassDependenciesAnalyzer analyzer, FileHasher fileHasher) {
        this.stash = stash;
        this.fileOperations = fileOperations;
        this.analyzer = analyzer;
        this.fileHasher = fileHasher;
    }

    public void updateAnalysis(JavaCompileSpec spec, WorkResult result) {
        if (result instanceof RecompilationNotNecessary) {
            return;
        }
        Timer clock = Time.startTimer();
        CompilationResultAnalyzer analyzer = new CompilationResultAnalyzer(this.analyzer, fileHasher);
        visitAnnotationProcessingResult(spec, result, analyzer);
        visitClassFiles(spec, analyzer);
        ClassSetAnalysisData data = analyzer.getAnalysis();
        stash.put(data);
        LOG.info("Class dependency analysis for incremental compilation took {}.", clock.getElapsed());
    }

    private void visitAnnotationProcessingResult(JavaCompileSpec spec, WorkResult result, CompilationResultAnalyzer analyzer) {
        Set<AnnotationProcessorDeclaration> processors = spec.getEffectiveAnnotationProcessors();
        if (processors != null && !processors.isEmpty()) {
            AnnotationProcessingResult annotationProcessingResult = null;
            if (result instanceof JdkJavaCompilerResult) {
                annotationProcessingResult = ((JdkJavaCompilerResult) result).getAnnotationProcessingResult();
            }
            analyzer.visitAnnotationProcessingResult(annotationProcessingResult);
        }
    }

    private void visitClassFiles(JavaCompileSpec spec, CompilationResultAnalyzer analyzer) {
        Set<File> baseDirs = Sets.newLinkedHashSet();
        baseDirs.add(spec.getDestinationDir());
        for (File baseDir : baseDirs) {
            fileOperations.fileTree(baseDir).visit(analyzer);
        }
    }
}
