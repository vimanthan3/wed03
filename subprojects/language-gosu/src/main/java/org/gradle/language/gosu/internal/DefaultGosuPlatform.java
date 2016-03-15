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

package org.gradle.language.gosu.internal;

import org.gradle.language.gosu.GosuPlatform;
import org.gradle.util.VersionNumber;

public class DefaultGosuPlatform implements GosuPlatform {
    private final String gosuVersion;

    public DefaultGosuPlatform(String gosuVersion) {
        this(VersionNumber.parse(gosuVersion));
    }

    public DefaultGosuPlatform(VersionNumber versionNumber) {
        this.gosuVersion = versionNumber.getMajor() + "." + versionNumber.getMinor() + "." + versionNumber.getMicro();
    }

    public String getGosuVersion() {
        return gosuVersion;
    }

    public String getDisplayName() {
        return String.format("Gosu %s", gosuVersion);
    }

    public String getName() {
        return String.format("GosuPlatform%s", gosuVersion);
    }
}
