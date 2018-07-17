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

package org.gradle.cache.internal;

import org.gradle.internal.concurrent.Stoppable;

import java.io.File;

public class VersionSpecificCacheAndWrapperDistributionCleanupService implements Stoppable {

    private static final long MAX_UNUSED_DAYS_FOR_RELEASES = 30;
    private static final long MAX_UNUSED_DAYS_FOR_SNAPSHOTS = 7;

    private final File gradleUserHomeDirectory;

    public VersionSpecificCacheAndWrapperDistributionCleanupService(File gradleUserHomeDirectory) {
        this.gradleUserHomeDirectory = gradleUserHomeDirectory;
    }

    @Override
    public void stop() {
        File cacheBaseDir = new File(gradleUserHomeDirectory, DefaultCacheScopeMapping.GLOBAL_CACHE_DIR_NAME);
        new VersionSpecificCacheCleanupAction(cacheBaseDir, MAX_UNUSED_DAYS_FOR_RELEASES, MAX_UNUSED_DAYS_FOR_SNAPSHOTS)
            .andThen(new WrapperDistributionCleanupAction(gradleUserHomeDirectory))
            .execute();
    }
}
