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

package org.gradle.jvm.toolchain.internal;

import org.gradle.api.GradleException;
import org.gradle.jvm.toolchain.JavaToolchainSpec;

public class NoToolchainAvailableException extends GradleException {

    public NoToolchainAvailableException(JavaToolchainSpec specification, boolean autoDetect, boolean autoDownload) {
        super("No compatible toolchains found for request specification: " + specification.getDisplayName() + " (auto-detect " + autoDetect + ", auto-download " + autoDownload + ")");
    }
}
