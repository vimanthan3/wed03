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

package org.gradle.api.internal.file.archive;

import org.gradle.api.file.FileVisitor;
import org.gradle.api.internal.file.FileTreeInternal;
import org.gradle.api.internal.file.collections.FileSystemMirroringFileTree;
import org.gradle.api.internal.tasks.TaskDependencyContainer;
import org.gradle.api.internal.tasks.TaskDependencyResolveContext;
import org.gradle.api.provider.Provider;
import org.gradle.cache.internal.DecompressionCache;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract base class for a {@link org.gradle.api.file.FileTree FileTree} that is backed by an archive file.
 *
 * Will decompress the archive file to the given cache.
 */
/* package */ abstract class AbstractArchiveFileTree<ENTRY, METADATA extends ArchiveMetadata<ENTRY>> implements FileSystemMirroringFileTree, TaskDependencyContainer {
    protected final DecompressionCache decompressionCache;

    protected AbstractArchiveFileTree(DecompressionCache decompressionCache) {
        this.decompressionCache = decompressionCache;
    }

    abstract protected Provider<File> getBackingFileProvider();

    private File getBackingFile() {
        return getBackingFileProvider().get();
    }

    @Override
    public void visitStructure(MinimalFileTreeStructureVisitor visitor, FileTreeInternal owner) {
        File backingFile = getBackingFile();
        visitor.visitFileTreeBackedByFile(backingFile, owner, this);
    }

    @Override
    public void visitDependencies(TaskDependencyResolveContext context) {
        context.add(getBackingFileProvider());
    }

    @Override
    public boolean isArchive() {
        return true;
    }

    abstract AbstractArchiveFileTreeElement<ENTRY, METADATA> createDetails(
        ENTRY entry,
        @Nullable String targetPath,
        boolean preserveLink,
        METADATA metadata,
        AtomicBoolean stopFlag
    );

    protected void visitEntry(ENTRY entry, METADATA metadata, FileVisitor visitor, boolean preserveLinks, AtomicBoolean stopFlag, boolean extract) {
        AbstractArchiveFileTreeElement<ENTRY, METADATA> details = createDetails(entry, null, preserveLinks, metadata, stopFlag);
        if (details.isDirectory()) {
            visitor.visitDir(details);
            if (!preserveLinks && details.isLink()) {
                @SuppressWarnings("DataFlowIssue") // targetEntry is not null for directories
                ENTRY targetEntry = details.getSymbolicLinkDetails().getTargetEntry();
                String originalPath = metadata.getPath(targetEntry);
                visitSymlinkedDirectory(originalPath, metadata.getPath(entry) + '/', metadata, visitor, preserveLinks, stopFlag);
            }
        } else {
            if (extract) {
                details.getFile();
            }
            visitor.visitFile(details);
        }
    }

    protected void visitSymlinkedEntry(ENTRY entry, String targetPath, METADATA metadata, FileVisitor visitor, boolean preserveLinks, AtomicBoolean stopFlag) {
        AbstractArchiveFileTreeElement<ENTRY, METADATA> details = createDetails(entry, targetPath, preserveLinks, metadata, stopFlag);
        if (details.isDirectory()) {
            visitor.visitDir(details);
            if (!preserveLinks && details.isLink()) {
                @SuppressWarnings("DataFlowIssue") // targetEntry is not null for directories
                ENTRY targetEntry = details.getSymbolicLinkDetails().getTargetEntry();
                String originalPath = metadata.getPath(targetEntry);
                visitSymlinkedDirectory(originalPath, targetPath + '/', metadata, visitor, preserveLinks, stopFlag);
            }
        } else {
            visitor.visitFile(details);
        }
    }

    private void visitSymlinkedDirectory(String originalPath, String targetPath, METADATA metadata, FileVisitor visitor, boolean preserveLinks, AtomicBoolean stopFlag) {
        String currentKey = originalPath;
        while (!stopFlag.get()) {
            Map.Entry<String, ENTRY> subEntry = metadata.getEntries().higherEntry(currentKey);
            if (subEntry != null && subEntry.getKey().startsWith(originalPath)) {
                currentKey = subEntry.getKey();
            } else {
                break;
            }

            String newPath = targetPath + currentKey.substring(originalPath.length());
            visitSymlinkedEntry(subEntry.getValue(), newPath, metadata, visitor, preserveLinks, stopFlag);
        }
    }
}
