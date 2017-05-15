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

package org.gradle.api.internal.changedetection.rules;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import org.gradle.api.Nullable;
import org.gradle.api.internal.TaskInternal;
import org.gradle.api.internal.changedetection.state.TaskExecution;
import org.gradle.api.internal.tasks.ContextAwareTaskAction;
import org.gradle.internal.classloader.ClassLoaderHierarchyHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

class TaskTypeTaskStateChanges extends SimpleTaskStateChanges {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskTypeTaskStateChanges.class);
    private final String taskPath;
    private final String taskClass;
    private final HashCode taskClassLoaderHash;
    private final List<HashCode> taskActionsClassLoaderHashes;
    private final List<String> taskActionsTypes;
    private final TaskExecution previousExecution;

    public TaskTypeTaskStateChanges(@Nullable TaskExecution previousExecution, TaskExecution currentExecution, String taskPath, Class<? extends TaskInternal> taskClass, Collection<ContextAwareTaskAction> taskActions, ClassLoaderHierarchyHasher classLoaderHierarchyHasher) {
        String taskClassName = taskClass.getName();
        currentExecution.setTaskClass(taskClassName);
        HashCode taskClassLoaderHash = classLoaderHierarchyHasher.getClassLoaderHash(taskClass.getClassLoader());
        currentExecution.setTaskClassLoaderHash(taskClassLoaderHash);
        List<HashCode> taskActionsClassLoaderHashes = collectActionClassLoaderHashes(taskActions, classLoaderHierarchyHasher);
        currentExecution.setTaskActionsClassLoaderHashes(taskActionsClassLoaderHashes);
        ImmutableList<String> taskActionsTypes = collectActionTypes(taskActions);
        currentExecution.setTaskActionsTypes(taskActionsTypes);
        LOGGER.debug("Task {} class loader hash: {}", taskPath, taskClassLoaderHash);
        LOGGER.debug("Task {} actions class loader hashes: {}", taskPath, taskActionsClassLoaderHashes);

        this.taskPath = taskPath;
        this.taskClass = taskClassName;
        this.taskClassLoaderHash = taskClassLoaderHash;
        this.taskActionsClassLoaderHashes = taskActionsClassLoaderHashes;
        this.taskActionsTypes = taskActionsTypes;
        this.previousExecution = previousExecution;
    }

    private static List<HashCode> collectActionClassLoaderHashes(Collection<ContextAwareTaskAction> taskActions, ClassLoaderHierarchyHasher classLoaderHierarchyHasher) {
        if (taskActions.isEmpty()) {
            return Collections.emptyList();
        }
        List<HashCode> hashCodes = Lists.newArrayListWithCapacity(taskActions.size());
        for (ContextAwareTaskAction taskAction : taskActions) {
            HashCode actionLoaderHash = classLoaderHierarchyHasher.getClassLoaderHash(taskAction.getClassLoader());
            hashCodes.add(actionLoaderHash);
        }
        return Collections.unmodifiableList(hashCodes);
    }

    private static ImmutableList<String> collectActionTypes(Collection<ContextAwareTaskAction> taskActions) {
        if (taskActions.isEmpty()) {
            return ImmutableList.of();
        }
        ImmutableList.Builder<String> types = ImmutableList.builder();
        for (ContextAwareTaskAction taskAction : taskActions) {
            types.add(taskAction.getActionType().getName());
        }
        return types.build();
    }

    @Override
    protected void addAllChanges(List<TaskStateChange> changes) {
        if (!taskClass.equals(previousExecution.getTaskClass())) {
            changes.add(new DescriptiveChange("Task '%s' has changed type from '%s' to '%s'.",
                    taskPath, previousExecution.getTaskClass(), taskClass));
            return;
        }
        if (taskClassLoaderHash == null) {
            changes.add(new DescriptiveChange("Task '%s' was loaded with an unknown classloader", taskPath));
            return;
        }
        if (previousExecution.getTaskClassLoaderHash() == null) {
            changes.add(new DescriptiveChange("Task '%s' was loaded with an unknown classloader during the previous execution", taskPath));
            return;
        }
        if (taskActionsClassLoaderHashes.contains(null)) {
            changes.add(new DescriptiveChange("Task '%s' has a custom action that was loaded with an unknown classloader", taskPath));
            return;
        }
        if (previousExecution.getTaskActionsClassLoaderHashes().contains(null)) {
            changes.add(new DescriptiveChange("Task '%s' had a custom action that was loaded with an unknown classloader during the previous execution", taskPath));
            return;
        }
        if (!taskClassLoaderHash.equals(previousExecution.getTaskClassLoaderHash())) {
            changes.add(new DescriptiveChange("Task '%s' class path has changed from %s to %s.", taskPath, previousExecution.getTaskClassLoaderHash(), taskClassLoaderHash));
            return;
        }
        if (!taskActionsClassLoaderHashes.equals(previousExecution.getTaskActionsClassLoaderHashes())) {
            changes.add(new DescriptiveChange("Task '%s' additional action class paths have changed from %s to %s.", taskPath, previousExecution.getTaskActionsClassLoaderHashes(), taskActionsClassLoaderHashes));
        }
        if (!taskActionsTypes.equals(previousExecution.getTaskActionsTypes())) {
            changes.add(new DescriptiveChange("Task '%s' additional action types have changed from %s to %s.", taskPath, previousExecution.getTaskActionsTypes(), taskActionsTypes));
        }
    }
}
