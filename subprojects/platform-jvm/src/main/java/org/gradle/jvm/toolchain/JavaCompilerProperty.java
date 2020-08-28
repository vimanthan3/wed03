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

package org.gradle.jvm.toolchain;

import org.gradle.api.Action;
import org.gradle.api.Incubating;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;

/**
 * A dedicated field type to deal with {@link JavaCompiler}.
 *
 * It enables the value to be set from a {@link JavaToolchainSpec} and exposes a {@link Provider} for accessing the value.
 *
 * @since 6.7
 */
@Incubating
public interface JavaCompilerProperty {

    /**
     * Sets the property value lazily from the provided {@link JavaToolchainSpec}
     *
     * @param toolchainConfig the {@code JavaToolchainSpec} to select the toolchain
     */
    void from(Action<? super JavaToolchainSpec> toolchainConfig);

    /**
     * Provides lazy access to the {@link JavaCompiler}.
     *
     * @return a {@code Provider<JavaCompiler>}
     */
    @Nested
    @Optional
    Provider<JavaCompiler> getProvider();
}
