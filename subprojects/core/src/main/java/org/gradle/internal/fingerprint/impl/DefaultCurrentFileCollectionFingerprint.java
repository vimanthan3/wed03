/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.internal.fingerprint.impl;

import org.gradle.api.internal.changedetection.rules.TaskStateChangeVisitor;
import org.gradle.api.internal.changedetection.state.NormalizedFileSnapshot;
import org.gradle.api.internal.changedetection.state.mirror.FileSystemSnapshot;
import org.gradle.api.internal.changedetection.state.mirror.PhysicalSnapshotVisitor;
import org.gradle.api.internal.changedetection.state.mirror.logical.FingerprintCompareStrategy;
import org.gradle.api.internal.changedetection.state.mirror.logical.FingerprintingStrategy;
import org.gradle.caching.internal.DefaultBuildCacheHasher;
import org.gradle.internal.fingerprint.CurrentFileCollectionFingerprint;
import org.gradle.internal.fingerprint.FileCollectionFingerprint;
import org.gradle.internal.fingerprint.HistoricalFileCollectionFingerprint;
import org.gradle.internal.hash.HashCode;

import javax.annotation.Nullable;
import java.util.Map;

public class DefaultCurrentFileCollectionFingerprint implements CurrentFileCollectionFingerprint {

    private final Map<String, NormalizedFileSnapshot> snapshots;
    private final FingerprintCompareStrategy compareStrategy;
    private final Iterable<FileSystemSnapshot> roots;
    private HashCode hash;

    public static CurrentFileCollectionFingerprint from(Iterable<FileSystemSnapshot> roots, FingerprintingStrategy strategy) {
        Map<String, NormalizedFileSnapshot> snapshots = strategy.collectSnapshots(roots);
        if (snapshots.isEmpty()) {
            return EmptyFileCollectionFingerprint.INSTANCE;
        }
        return new DefaultCurrentFileCollectionFingerprint(snapshots, strategy.getCompareStrategy(), roots);
    }

    private DefaultCurrentFileCollectionFingerprint(Map<String, NormalizedFileSnapshot> snapshots, FingerprintCompareStrategy compareStrategy, @Nullable Iterable<FileSystemSnapshot> roots) {
        this.snapshots = snapshots;
        this.compareStrategy = compareStrategy;
        this.roots = roots;
    }

    @Override
    public boolean visitChangesSince(FileCollectionFingerprint oldFingerprint, String title, boolean includeAdded, TaskStateChangeVisitor visitor) {
        if (hash != null && hash.equals(oldFingerprint.getHash())) {
            return true;
        }
        return compareStrategy.visitChangesSince(visitor, getSnapshots(), oldFingerprint.getSnapshots(), title, includeAdded);
    }

    @Override
    public HashCode getHash() {
        if (hash == null) {
            DefaultBuildCacheHasher hasher = new DefaultBuildCacheHasher();
            compareStrategy.appendToHasher(hasher, snapshots);
            hash = hasher.hash();
        }
        return hash;
    }

    @Override
    public Map<String, NormalizedFileSnapshot> getSnapshots() {
        return snapshots;
    }

    @Override
    public void visitRoots(PhysicalSnapshotVisitor visitor) {
        if (roots == null) {
            throw new UnsupportedOperationException("Roots not available.");
        }
        for (FileSystemSnapshot root : roots) {
            root.accept(visitor);
        }
    }

    @Override
    public HistoricalFileCollectionFingerprint archive() {
        return new DefaultHistoricalFileCollectionFingerprint(snapshots, compareStrategy, hash);
    }
}
