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

package org.gradle.api.internal.file.archive;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;

public abstract class ArchiveMetadata<ENTRY> {
    private final File originalFile;
    protected final File expandedDir;
    protected TreeMap<String, ENTRY> entries = null;

    ArchiveMetadata(
        File originalFile,
        File expandedDir
    ) {
        this.originalFile = originalFile;
        this.expandedDir = expandedDir;
    }

    abstract TreeMap<String, ENTRY> getEntries();

    abstract boolean isSymlink(ENTRY entry);

    abstract boolean isDirectory(ENTRY entry);

    abstract String getPath(ENTRY entry);

    abstract String getSymlinkTarget(ENTRY entry);

    abstract int getUnixMode(ENTRY entry);

    abstract long getLastModifiedTime(ENTRY entry);

    abstract long getSize(ENTRY entry);

    abstract @Nullable ENTRY getEntry(String path);

    @Nullable
    ENTRY getTargetEntry(ENTRY entry) {
        String path = getPath(entry);
        ArrayList<String> parts = new ArrayList<>(Arrays.asList(path.split("/")));
        if (getTargetFollowingLinks(entry, parts, entry)) {
            String targetPath = String.join("/", parts);
            ENTRY targetEntry = getEntry(targetPath);
            if (targetEntry == null) { //retry for directories
                targetEntry = getEntry(targetPath + "/");
            }
            return targetEntry;
        } else {
            return null;
        }
    }

    private boolean getTargetFollowingLinks(ENTRY entry, ArrayList<String> parts, ENTRY originalEntry) {
        parts.remove(parts.size() - 1);
        String target = getSymlinkTarget(entry);
        for (String targetPart : target.split("/")) {
            if (targetPart.equals("..")) {
                if (parts.isEmpty()) {
                    return false;
                }
                parts.remove(parts.size() - 1);
            } else if (targetPart.equals(".")) {
                continue;
            } else {
                parts.add(targetPart);
                String currentPath = String.join("/", parts);
                ENTRY currentEntry = getEntry(currentPath);
                if (currentEntry != null && isSymlink(currentEntry)) {
                    if (currentEntry.equals(originalEntry)) {
                        return false; //cycle
                    }
                    boolean success = getTargetFollowingLinks(currentEntry, parts, originalEntry);
                    if (!success) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public File getOriginalFile() {
        return originalFile;
    }
}
