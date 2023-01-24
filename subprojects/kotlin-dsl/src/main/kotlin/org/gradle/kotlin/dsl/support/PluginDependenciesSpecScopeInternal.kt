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

package org.gradle.kotlin.dsl.support

import org.gradle.api.internal.catalog.ExternalModuleDependencyFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.*
import org.gradle.plugin.use.PluginDependenciesSpec


internal
class PluginDependenciesSpecScopeInternal(
    private val scriptTarget: ExtensionAware,
    plugins: PluginDependenciesSpec
) : PluginDependenciesSpecScope(plugins) {

    /**
     * Used by version catalog accessors in project scripts plugins {} block.
     */
    @Suppress("UNUSED")
    fun versionCatalogExtension(name: String): ExternalModuleDependencyFactory =
        scriptTarget.extensions[name] as ExternalModuleDependencyFactory
}
