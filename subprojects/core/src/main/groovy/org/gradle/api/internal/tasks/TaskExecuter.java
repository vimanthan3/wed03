/*
 * Copyright 2010 the original author or authors.
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
package org.gradle.api.internal.tasks;

import org.gradle.api.internal.TaskInternal;

public interface TaskExecuter {
    /**
     * Executes the given task. If the task fails with an exception, the exception is packaged in the provided task
     * state.
     */
    void execute(TaskInternal task, TaskStateInternal state, TaskExecutionContext context);

    /**
     * Tells whether the task is currently up-to-date and in need of re-running
     * Note that this could potentially change after running other dependency steps
     */
    boolean isCurrentlyUpToDate(TaskInternal task, TaskStateInternal state);
}
