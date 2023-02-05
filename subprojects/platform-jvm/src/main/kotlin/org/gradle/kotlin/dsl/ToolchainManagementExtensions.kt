/*
 * Copyright 2022 the original author or authors.
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

package org.gradle.kotlin.dsl

import org.gradle.api.Incubating
import org.gradle.api.toolchain.management.ToolchainManagement
import org.gradle.internal.deprecation.DeprecationLogger
import org.gradle.jvm.toolchain.JvmToolchainManagement


/**
 * Provides statically defined accessors for configuring the "jvm" block in "toolchainManagement".
 * The "jvm-toolchain-management" plugin needs to be applied in order for these extensions to work.
 *
 * @since 7.6
 */
@Incubating
@Deprecated(
    message = "Prefer generated type-safe accessors or use the API. Will be removed in 9.0.",
    replaceWith = ReplaceWith(
        "configure<JvmToolchainManagement> {}",
        "org.gradle.jvm.toolchain.JvmToolchainManagement"
    ),
    level = DeprecationLevel.HIDDEN,
)
fun ToolchainManagement.jvm(block: JvmToolchainManagement.() -> Unit) {
    logDeprecation()
    extensions.configure(JvmToolchainManagement::class.java, block)
}


/**
 * Provides statically defined accessors for getting the "jvm" block of "toolchainManagement".
 * The "jvm-toolchain-management" plugin needs to be applied in order for these extensions to work.
 *
 * @since 7.6
 */
@get:Incubating
@Deprecated(
    message = "Prefer generated type-safe accessors or use the API. Will be removed in 9.0.",
    replaceWith = ReplaceWith(
        "the<JvmToolchainManagement>()",
        "org.gradle.jvm.toolchain.JvmToolchainManagement"
    ),
    level = DeprecationLevel.HIDDEN,
)
val ToolchainManagement.jvm: JvmToolchainManagement
    get() {
        logDeprecation()
        return extensions.getByType(JvmToolchainManagement::class.java)
    }


private
fun logDeprecation() {
    DeprecationLogger.deprecate("import org.gradle.kotlin.dsl.jvm")
        .willBeRemovedInGradle9()
        .undocumented()
        .nagUser()
}
