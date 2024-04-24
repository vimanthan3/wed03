/*
 * Copyright 2023 the original author or authors.
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

package org.gradle.problems.buildtree;

import com.google.common.base.Supplier;
import org.gradle.problems.ProblemDiagnostics;

import javax.annotation.Nullable;

public interface ProblemStream {

    /**
     * Returns diagnostics based on the state of the calling thread.
     */
    ProblemDiagnostics forCurrentCaller();

    /**
     * Returns diagnostics based on the state of the calling thread.
     *
     * @param exception The exception that represents the failure.
     */
    ProblemDiagnostics forCurrentCaller(@Nullable Throwable exception);

    /**
     * Returns diagnostics based on the state of the calling thread.
     *
     * @param exceptionFactory The factory to use to produce an exception when a stack trace is required.
     */
    ProblemDiagnostics forCurrentCaller(Supplier<? extends Throwable> exceptionFactory);

}
