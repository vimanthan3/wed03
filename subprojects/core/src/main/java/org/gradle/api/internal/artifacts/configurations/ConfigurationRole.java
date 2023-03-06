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

package org.gradle.api.internal.artifacts.configurations;

import org.gradle.internal.deprecation.DeprecationLogger;

import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * Defines how a {@link org.gradle.api.artifacts.Configuration} is intended to be used.
 *
 * Standard roles are defined in {@link ConfigurationRoles}.
 *
 * @since 8.1
 */
public interface ConfigurationRole {
    /**
     * Returns a human-readable name for this role.
     */
    String getName();

    boolean isConsumable();
    boolean isResolvable();
    boolean isDeclarableAgainst();
    boolean isConsumptionDeprecated();
    boolean isResolutionDeprecated();
    boolean isDeclarationAgainstDeprecated();

    /**
     * Obtains a human-readable summary of the usage allowed by the given role.
     */
    default String describeUsage() {
        return UsageDescriber.describeRole(this);
    }

    /**
     * Attempts to locate a pre-defined role allowing the given usage in {@link ConfigurationRoles} and return it;
     * if such a roles does not exist, creates a custom anonymous implementation and returns it instead.
     *
     * @param name the name to use when creating a role that wasn't found
     * @param consumable whether this role is consumable
     * @param resolvable whether this role is resolvable
     * @param declarableAgainst whether this role is declarable against
     * @param consumptionDeprecated whether this role is deprecated for consumption
     * @param resolutionDeprecated whether this role is deprecated for resolution
     * @param declarationAgainstDeprecated whether this role is deprecated for declaration against
     * @param description a custom description to use when creating a role that wasn't found; or {@code null} to use the default description generated by {@link UsageDescriber}
     * @param warnOnCustomRole {@code true} to warn if this call results in the creation of a custom role; {@code false} otherwise
     * @return a role with matching usage characteristics
     */
    static ConfigurationRole forUsage(String name, boolean consumable, boolean resolvable, boolean declarableAgainst, boolean consumptionDeprecated, boolean resolutionDeprecated, boolean declarationAgainstDeprecated, @Nullable String description, boolean warnOnCustomRole) {
        if ((!consumable && consumptionDeprecated) || (!resolvable && resolutionDeprecated) || (!declarableAgainst && declarationAgainstDeprecated)) {
            throw new IllegalArgumentException("Cannot create a role that deprecates a usage that is not allowed");
        }

        ConfigurationRole result = ConfigurationRoles.byUsage(consumable, resolvable, declarableAgainst, consumptionDeprecated, resolutionDeprecated, declarationAgainstDeprecated)
                .map(ConfigurationRole.class::cast)
                .orElse(new CustomConfigurationRole(name, consumable, resolvable, declarableAgainst, consumptionDeprecated, resolutionDeprecated, declarationAgainstDeprecated, description));

        //noinspection SuspiciousMethodCalls
        if (warnOnCustomRole && !Arrays.asList(ConfigurationRoles.values()).contains(result)) {
            DeprecationLogger.deprecateBehaviour("Custom configuration roles are deprecated.")
                    .withAdvice("Use one of the standard roles defined in ConfigurationRoles instead.")
                    .willBeRemovedInGradle9()
                    .withUpgradeGuideSection(8, "custom_configuration_roles")
                    .nagUser();
        }

        return result;
    }

    static ConfigurationRole forUsage(boolean consumable, boolean resolvable, boolean declarableAgainst, boolean consumptionDeprecated, boolean resolutionDeprecated, boolean declarationAgainstDeprecated) {
        return forUsage(UsageDescriber.DEFAULT_CUSTOM_ROLE_NAME, consumable, resolvable, declarableAgainst, consumptionDeprecated, resolutionDeprecated, declarationAgainstDeprecated);
    }
    static ConfigurationRole forUsage(String name, boolean consumable, boolean resolvable, boolean declarableAgainst, boolean consumptionDeprecated, boolean resolutionDeprecated, boolean declarationAgainstDeprecated) {
        return forUsage(name, consumable, resolvable, declarableAgainst, consumptionDeprecated, resolutionDeprecated, declarationAgainstDeprecated, null, false);
    }

    /**
     * A custom implementation of {@link ConfigurationRole} that allows for non-standard combinations of usage characteristics and deprecations.
     */
    final class CustomConfigurationRole implements ConfigurationRole {
        private final String name;
        private final boolean consumable;
        private final boolean resolvable;
        private final boolean declarableAgainst;
        private final boolean consumptionDeprecated;
        private final boolean resolutionDeprecated;
        private final boolean declarationAgainstDeprecated;
        @Nullable
        private final String description;

        private CustomConfigurationRole(String name, boolean consumable, boolean resolvable, boolean declarableAgainst, boolean consumptionDeprecated, boolean resolutionDeprecated, boolean declarationAgainstDeprecated, @Nullable String description) {
            this.name = name;
            this.consumable = consumable;
            this.resolvable = resolvable;
            this.declarableAgainst = declarableAgainst;
            this.consumptionDeprecated = consumptionDeprecated;
            this.resolutionDeprecated = resolutionDeprecated;
            this.declarationAgainstDeprecated = declarationAgainstDeprecated;
            this.description = description;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isConsumable() {
            return consumable;
        }

        @Override
        public boolean isResolvable() {
            return resolvable;
        }

        @Override
        public boolean isDeclarableAgainst() {
            return declarableAgainst;
        }

        @Override
        public boolean isConsumptionDeprecated() {
            return consumptionDeprecated;
        }

        @Override
        public boolean isResolutionDeprecated() {
            return resolutionDeprecated;
        }

        @Override
        public boolean isDeclarationAgainstDeprecated() {
            return declarationAgainstDeprecated;
        }

        @Override
        public String describeUsage() {
            if (description != null) {
                return description;
            } else {
                return UsageDescriber.describeRole(this);
            }
        }
    }
}
