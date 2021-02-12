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
package org.gradle.internal.compiler.java;

import com.sun.source.util.JavacTask;

import javax.annotation.processing.Processor;
import javax.tools.JavaCompiler;
import java.io.File;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This is a Java compiler plugin, which must be loaded in the same classloader
 * as the one which loads the JDK compiler itself. For this reason this task lives
 * in its own subproject and uses as little dependencies as possible (in particular
 * it only depends on JDK types).
 *
 * This class is therefore loaded (and tested) via reflection in org.gradle.api.internal.tasks.compile.JdkTools.
 */
@SuppressWarnings("unused")
public class IncrementalCompileTask implements JavaCompiler.CompilationTask {

    private final Function<File, Optional<String>> relativize;
    private final Consumer<Map<String, Collection<String>>> onComplete;
    private final JavacTask delegate;

    public IncrementalCompileTask(JavaCompiler.CompilationTask delegate,
                                  Function<File, Optional<String>> relativize,
                                  Consumer<Map<String, Collection<String>>> onComplete) {
        this.relativize = relativize;
        this.onComplete = onComplete;
        if (delegate instanceof JavacTask) {
            this.delegate = (JavacTask) delegate;
        } else {
            throw new UnsupportedOperationException("Unexpected Java compile task : " + delegate.getClass().getName());
        }
    }

    @Override
    public void addModules(Iterable<String> moduleNames) {
        delegate.addModules(moduleNames);
    }

    @Override
    public void setProcessors(Iterable<? extends Processor> processors) {
        delegate.setProcessors(processors);
    }

    @Override
    public void setLocale(Locale locale) {
        delegate.setLocale(locale);
    }

    @Override
    public Boolean call() {
        ClassNameCollector collector = new ClassNameCollector(relativize, delegate.getElements());
        delegate.addTaskListener(collector);
        try {
            return delegate.call();
        } finally {
            onComplete.accept(collector.getMapping());
        }
    }

}
