/*
 * Copyright 2010 the original author or authors.
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
package org.gradle.api.internal.initialization;

import groovy.lang.Closure;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.dsl.DependencyLockingHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.initialization.dsl.ScriptHandler;
import org.gradle.api.internal.DynamicObjectAware;
import org.gradle.api.internal.artifacts.DependencyResolutionServices;
import org.gradle.api.internal.artifacts.JavaEcosystemSupport;
import org.gradle.api.internal.artifacts.configurations.ConfigurationRolesForMigration;
import org.gradle.api.internal.artifacts.configurations.RoleBasedConfigurationContainerInternal;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.groovy.scripts.ScriptSource;
import org.gradle.internal.classloader.ClasspathUtil;
import org.gradle.internal.classpath.ClassPath;
import org.gradle.internal.metaobject.BeanDynamicObject;
import org.gradle.internal.metaobject.DynamicObject;
import org.gradle.internal.resource.ResourceLocation;
import org.gradle.util.internal.ConfigureUtil;

import java.io.File;
import java.net.URI;

public class DefaultScriptHandler implements ScriptHandler, ScriptHandlerInternal, DynamicObjectAware {

    /**
     * If set to {@code true}, the buildscript's {@code classpath} configuration will not be reset after the
     * classpath has been assembled. Defaults to {@code false}.
     */
    public static final String DISABLE_RESET_CONFIGURATION_SYSTEM_PROPERTY = "org.gradle.incubating.reset-buildscript-classpath.disabled";

    private static final Logger LOGGER = Logging.getLogger(DefaultScriptHandler.class);

    private final ResourceLocation scriptResource;
    private final ClassLoaderScope classLoaderScope;
    private final ScriptClassPathResolver scriptClassPathResolver;
    private final DependencyResolutionServices dependencyResolutionServices;

    // The following values are relatively expensive to create, so defer creation until required
    private ClassPath resolvedClasspath;
    private RepositoryHandler repositoryHandler;
    private DependencyHandler dependencyHandler;
    private RoleBasedConfigurationContainerInternal configContainer;
    private Configuration classpathConfiguration;
    private DynamicObject dynamicObject;

    public DefaultScriptHandler(ScriptSource scriptSource, DependencyResolutionServices dependencyResolutionServices, ClassLoaderScope classLoaderScope, ScriptClassPathResolver scriptClassPathResolver) {
        this.dependencyResolutionServices = dependencyResolutionServices;
        this.scriptResource = scriptSource.getResource().getLocation();
        this.classLoaderScope = classLoaderScope;
        this.scriptClassPathResolver = scriptClassPathResolver;
        JavaEcosystemSupport.configureSchema(dependencyResolutionServices.getAttributesSchema(), dependencyResolutionServices.getObjectFactory());
    }

    @Override
    public void dependencies(Closure configureClosure) {
        ConfigureUtil.configure(configureClosure, getDependencies());
    }

    @Override
    public void addScriptClassPathDependency(Object notation) {
        getDependencies().add(ScriptHandler.CLASSPATH_CONFIGURATION, notation);
    }

    @Override
    public ClassPath getScriptClassPath() {
        return ClasspathUtil.getClasspath(getClassLoader());
    }

    @Override
    public ClassPath getInstrumentedScriptClassPath() {
        if (resolvedClasspath == null) {
            // There's a side effect of creating a dependency locking handler that's needed for writing verification metadata
            getDependencyLocking();

            resolvedClasspath = scriptClassPathResolver.resolveClassPath(classpathConfiguration);
            if (!System.getProperty(DISABLE_RESET_CONFIGURATION_SYSTEM_PROPERTY, "false").equals("true") && classpathConfiguration != null) {
                ((ResettableConfiguration) classpathConfiguration).resetResolutionState();
            }
        }
        return resolvedClasspath;
    }

    @Override
    public DependencyHandler getDependencies() {
        defineConfiguration();
        return dependencyHandler;
    }

    @Override
    public RepositoryHandler getRepositories() {
        if (repositoryHandler == null) {
            repositoryHandler = dependencyResolutionServices.getResolveRepositoryHandler();
        }
        return repositoryHandler;
    }

    @Override
    public void repositories(Closure configureClosure) {
        ConfigureUtil.configure(configureClosure, getRepositories());
    }

    @Override
    public ConfigurationContainer getConfigurations() {
        defineConfiguration();
        return configContainer;
    }

    @SuppressWarnings("deprecation")
    private void defineConfiguration() {
        // Defer creation and resolution of configuration until required. Short-circuit when script does not require classpath
        if (configContainer == null) {
            configContainer = (RoleBasedConfigurationContainerInternal) dependencyResolutionServices.getConfigurationContainer();
        }
        if (dependencyHandler == null) {
            dependencyHandler = dependencyResolutionServices.getDependencyHandler();
        }
        if (classpathConfiguration == null) {
            classpathConfiguration = configContainer.createWithRole(CLASSPATH_CONFIGURATION, ConfigurationRolesForMigration.LEGACY_TO_RESOLVABLE_BUCKET);
            scriptClassPathResolver.prepareClassPath(classpathConfiguration, dependencyHandler);
        }
    }

    @Override
    public void dependencyLocking(Closure configureClosure) {
        ConfigureUtil.configure(configureClosure, getDependencyLocking());
    }

    @Override
    public DependencyLockingHandler getDependencyLocking() {
        return dependencyResolutionServices.getDependencyLockingHandler();
    }

    @Override
    public File getSourceFile() {
        return scriptResource.getFile();
    }

    @Override
    public URI getSourceURI() {
        return scriptResource.getURI();
    }

    @Override
    public ClassLoader getClassLoader() {
        if (!classLoaderScope.isLocked()) {
            LOGGER.debug("Eager creation of script class loader for {}. This may result in performance issues.", scriptResource.getDisplayName());
        }
        return classLoaderScope.getLocalClassLoader();
    }

    @Override
    public DynamicObject getAsDynamicObject() {
        if (dynamicObject == null) {
            dynamicObject = new BeanDynamicObject(this);
        }
        return dynamicObject;
    }

    @Override
    public String toString() {
        return (resolvedClasspath == null ? "unresolved" : "resolved") + " script handler for " + getSourceFile();
    }
}
