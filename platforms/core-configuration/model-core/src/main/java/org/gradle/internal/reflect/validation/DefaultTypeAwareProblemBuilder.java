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

package org.gradle.internal.reflect.validation;

import org.gradle.api.NonNullApi;
import org.gradle.api.problems.internal.InternalProblemBuilder;
import org.gradle.api.problems.internal.Problem;
import org.gradle.api.problems.internal.TypeValidationData;

import javax.annotation.Nullable;
import java.util.Optional;

import static java.lang.Boolean.TRUE;
import static java.lang.Boolean.parseBoolean;

@NonNullApi
public class DefaultTypeAwareProblemBuilder extends DelegatingProblemBuilder implements TypeAwareProblemBuilder {

    public DefaultTypeAwareProblemBuilder(InternalProblemBuilder problemBuilder) {
        super(problemBuilder);
    }

    @Override
    public TypeAwareProblemBuilder withAnnotationType(@Nullable Class<?> classWithAnnotationAttached) {
        if (classWithAnnotationAttached != null) {
            additionalData(TypeValidationData.class, spec -> spec.typeName(classWithAnnotationAttached.getName().replaceAll("\\$", ".")));
        }
        return this;
    }

    @Override
    public TypeAwareProblemBuilder typeIsIrrelevantInErrorMessage() {
        additionalData(TypeValidationData.class, spec -> spec.isIrrelevantInErrorMessage(TRUE.toString()));
        return this;
    }

    @Override
    public TypeAwareProblemBuilder forProperty(String propertyName) {
        additionalData(TypeValidationData.class, spec -> spec.propertyName(propertyName));
        return this;
    }

    @Override
    public TypeAwareProblemBuilder parentProperty(@Nullable String parentProperty) {
        if (parentProperty == null) {
            return this;
        }
        String pp = getParentProperty(parentProperty);
        additionalData(TypeValidationData.class, spec -> spec.parentPropertyName(pp));
        parentPropertyAdditionalData = pp;
        return this;
    }

    @Override
    public Problem build() {
        // TODO (donat) TypeAwareProblemBuilder should have a know additionalData type to avoid unchecked casts
        Problem problem = super.build();
        @SuppressWarnings("unchecked")
        Optional<TypeValidationData> additionalData = Optional.ofNullable((TypeValidationData) problem.getAdditionalData());
        String prefix = introductionFor(additionalData);
        String text = Optional.ofNullable(problem.getContextualLabel()).orElseGet(() -> problem.getDefinition().getId().getDisplayName());
        return problem.toBuilder().contextualLabel(prefix + text).build();
    }

    public static String introductionFor(Optional<TypeValidationData> additionalData) {
        Optional<String> rootType = additionalData.map(TypeValidationData::getTypeName)
            .map(Object::toString)
            .filter(DefaultTypeAwareProblemBuilder::shouldRenderType);
        Optional<DefaultPluginId> pluginId = additionalData.map(TypeValidationData::getPluginId)
            .map(Object::toString)
            .map(DefaultPluginId::new);

        StringBuilder builder = new StringBuilder();
        boolean typeRelevant = rootType.isPresent() && !parseBoolean(additionalData.map(TypeValidationData::isIrrelevantInErrorMessage).orElse("").toString());
        if (typeRelevant) {
            if (pluginId.isPresent()) {
                builder.append("In plugin '")
                    .append(pluginId.get())
                    .append("' type '");
            } else {
                builder.append("Type '");
            }
            builder.append(rootType.get()).append("' ");
        }

        Object property = additionalData.map(TypeValidationData::getPropertyName).orElse(null);
        if (property != null) {
            if (typeRelevant) {
                builder.append("property '");
            } else {
                if (pluginId.isPresent()) {
                    builder.append("In plugin '")
                        .append(pluginId.get())
                        .append("' property '");
                } else {
                    builder.append("Property '");
                }
            }
            additionalData.map(TypeValidationData::getParentPropertyName).ifPresent(parentProperty -> {
                builder.append(parentProperty);
                builder.append('.');
            });
            builder.append(property)
                .append("' ");
        }
        return builder.toString();
    }

    // A heuristic to determine if the type is relevant or not.
    // The "DefaultTask" type may appear in error messages
    // (if using "adhoc" tasks) but isn't visible to this
    // class so we have to rely on text matching for now.
    private static boolean shouldRenderType(String className) {
        return !"org.gradle.api.DefaultTask".equals(className);
    }

    private String parentPropertyAdditionalData = null;

    private String getParentProperty(String parentProperty) {
        String existingParentProperty = parentPropertyAdditionalData;
        if (existingParentProperty == null) {
            return parentProperty;
        }
        return existingParentProperty + "." + parentProperty;
    }
}
