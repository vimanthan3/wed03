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

package org.gradle.tooling.internal.provider.runner;

import org.gradle.api.NonNullApi;
import org.gradle.api.problems.ProblemGroup;
import org.gradle.api.problems.ProblemId;
import org.gradle.api.problems.Severity;
import org.gradle.api.problems.internal.AdditionalData;
import org.gradle.api.problems.internal.AdditionalDataSpec;
import org.gradle.api.problems.internal.DefaultProblemProgressDetails;
import org.gradle.api.problems.internal.DeprecationData;
import org.gradle.api.problems.internal.DocLink;
import org.gradle.api.problems.internal.FileLocation;
import org.gradle.api.problems.internal.GenericData;
import org.gradle.api.problems.internal.LineInFileLocation;
import org.gradle.api.problems.internal.OffsetInFileLocation;
import org.gradle.api.problems.internal.PluginIdLocation;
import org.gradle.api.problems.internal.Problem;
import org.gradle.api.problems.internal.ProblemDefinition;
import org.gradle.api.problems.internal.ProblemLocation;
import org.gradle.api.problems.internal.TaskPathLocation;
import org.gradle.api.problems.internal.TypeValidationData;
import org.gradle.internal.build.event.types.DefaultAdditionalData;
import org.gradle.internal.build.event.types.DefaultContextualLabel;
import org.gradle.internal.build.event.types.DefaultDeprecationData;
import org.gradle.internal.build.event.types.DefaultDetails;
import org.gradle.internal.build.event.types.DefaultDocumentationLink;
import org.gradle.internal.build.event.types.DefaultFailure;
import org.gradle.internal.build.event.types.DefaultProblemDefinition;
import org.gradle.internal.build.event.types.DefaultProblemDescriptor;
import org.gradle.internal.build.event.types.DefaultProblemDetails;
import org.gradle.internal.build.event.types.DefaultProblemEvent;
import org.gradle.internal.build.event.types.DefaultProblemGroup;
import org.gradle.internal.build.event.types.DefaultProblemId;
import org.gradle.internal.build.event.types.DefaultSeverity;
import org.gradle.internal.build.event.types.DefaultSolution;
import org.gradle.internal.build.event.types.DefaultTypeValidationData;
import org.gradle.internal.operations.OperationIdentifier;
import org.gradle.internal.operations.OperationProgressEvent;
import org.gradle.tooling.internal.protocol.InternalFailure;
import org.gradle.tooling.internal.protocol.InternalProblemDefinition;
import org.gradle.tooling.internal.protocol.InternalProblemEventVersion2;
import org.gradle.tooling.internal.protocol.InternalProblemGroup;
import org.gradle.tooling.internal.protocol.InternalProblemId;
import org.gradle.tooling.internal.protocol.events.InternalProblemDescriptor;
import org.gradle.tooling.internal.protocol.problem.InternalAdditionalData;
import org.gradle.tooling.internal.protocol.problem.InternalContextualLabel;
import org.gradle.tooling.internal.protocol.problem.InternalDeprecationData;
import org.gradle.tooling.internal.protocol.problem.InternalDetails;
import org.gradle.tooling.internal.protocol.problem.InternalDocumentationLink;
import org.gradle.tooling.internal.protocol.problem.InternalLocation;
import org.gradle.tooling.internal.protocol.problem.InternalSeverity;
import org.gradle.tooling.internal.protocol.problem.InternalSolution;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toMap;

@NonNullApi
public class ProblemsProgressEventConsumer extends ClientForwardingBuildOperationListener {

    private static final InternalSeverity ADVICE = new DefaultSeverity(0);
    private static final InternalSeverity WARNING = new DefaultSeverity(1);
    private static final InternalSeverity ERROR = new DefaultSeverity(2);

    private final Supplier<OperationIdentifier> operationIdentifierSupplier;
    private final AggregatingProblemConsumer aggregator;

    ProblemsProgressEventConsumer(ProgressEventConsumer progressEventConsumer, Supplier<OperationIdentifier> operationIdentifierSupplier, AggregatingProblemConsumer aggregator) {
        super(progressEventConsumer);
        this.operationIdentifierSupplier = operationIdentifierSupplier;
        this.aggregator = aggregator;
    }

    @Override
    public void progress(OperationIdentifier buildOperationId, OperationProgressEvent progressEvent) {
        Object details = progressEvent.getDetails();
        createProblemEvent(buildOperationId, details)
            .ifPresent(aggregator::emit);
    }

    private Optional<InternalProblemEventVersion2> createProblemEvent(OperationIdentifier buildOperationId, @Nullable Object details) {
        if (details instanceof DefaultProblemProgressDetails) {
            Problem problem = ((DefaultProblemProgressDetails) details).getProblem();
            return Optional.of(createProblemEvent(buildOperationId, problem));
        }
        return empty();
    }

    private InternalProblemEventVersion2 createProblemEvent(OperationIdentifier buildOperationId, Problem problem) {
        return new DefaultProblemEvent(
            createDefaultProblemDescriptor(buildOperationId),
            new DefaultProblemDetails(
                toInternalDefinition(problem.getDefinition()),
                toInternalDetails(problem.getDetails()),
                toInternalContextualLabel(problem.getContextualLabel()),
                toInternalLocations(problem.getLocations()),
                toInternalSolutions(problem.getSolutions()),
                toInternalAdditionalData(problem.getAdditionalData()),
                toInternalFailure(problem.getException())
            )
        );
    }

    @Nullable
    private static InternalFailure toInternalFailure(@Nullable RuntimeException ex) {
        if (ex == null) {
            return null;
        }
        return DefaultFailure.fromThrowable(ex);
    }

    private InternalProblemDescriptor createDefaultProblemDescriptor(OperationIdentifier parentBuildOperationId) {
        return new DefaultProblemDescriptor(
            operationIdentifierSupplier.get(),
            parentBuildOperationId);
    }

    private static InternalProblemDefinition toInternalDefinition(ProblemDefinition definition) {
        return new DefaultProblemDefinition(
            toInternalId(definition.getId()),
            toInternalSeverity(definition.getSeverity()),
            toInternalDocumentationLink(definition.getDocumentationLink())
        );
    }

    private static InternalProblemId toInternalId(ProblemId problemId) {
        return new DefaultProblemId(problemId.getName(), problemId.getDisplayName(), toInternalGroup(problemId.getGroup()));
    }

    private static InternalProblemGroup toInternalGroup(ProblemGroup group) {
        return new DefaultProblemGroup(group.getName(), group.getDisplayName(), group.getParent() == null ? null : toInternalGroup(group.getParent()));
    }

    private static @Nullable InternalContextualLabel toInternalContextualLabel(@Nullable String contextualLabel) {
        return contextualLabel == null ? null : new DefaultContextualLabel(contextualLabel);
    }

    private static @Nullable InternalDetails toInternalDetails(@Nullable String details) {
        return details == null ? null : new DefaultDetails(details);
    }

    private static InternalSeverity toInternalSeverity(Severity severity) {
        switch (severity) {
            case ADVICE:
                return ADVICE;
            case WARNING:
                return WARNING;
            case ERROR:
                return ERROR;
            default:
                throw new RuntimeException("No mapping defined for severity level " + severity);
        }
    }

    private static List<InternalLocation> toInternalLocations(List<ProblemLocation> locations) {
        return locations.stream().map(location -> {
            if (location instanceof LineInFileLocation) {
                LineInFileLocation fileLocation = (LineInFileLocation) location;
                return new org.gradle.internal.build.event.types.DefaultLineInFileLocation(fileLocation.getPath(), fileLocation.getLine(), fileLocation.getColumn(), fileLocation.getLength());
            } else if (location instanceof OffsetInFileLocation) {
                OffsetInFileLocation fileLocation = (OffsetInFileLocation) location;
                return new org.gradle.internal.build.event.types.DefaultOffsetInFileLocation(fileLocation.getPath(), fileLocation.getOffset(), fileLocation.getLength());
            } else if (location instanceof FileLocation) { // generic class must be after the subclasses in the if-elseif chain.
                FileLocation fileLocation = (FileLocation) location;
                return new org.gradle.internal.build.event.types.DefaultFileLocation(fileLocation.getPath());
            } else if (location instanceof PluginIdLocation) {
                PluginIdLocation pluginLocation = (PluginIdLocation) location;
                return new org.gradle.internal.build.event.types.DefaultPluginIdLocation(pluginLocation.getPluginId());
            } else if (location instanceof TaskPathLocation) {
                TaskPathLocation taskLocation = (TaskPathLocation) location;
                return new org.gradle.internal.build.event.types.DefaultTaskPathLocation(taskLocation.getBuildTreePath());
            } else {
                throw new RuntimeException("No mapping defined for " + location.getClass().getName());
            }
        }).collect(toImmutableList());
    }

    @Nullable
    private static InternalDocumentationLink toInternalDocumentationLink(@Nullable DocLink link) {
        return (link == null || link.getUrl() == null) ? null : new DefaultDocumentationLink(link.getUrl());
    }

    private static List<InternalSolution> toInternalSolutions(List<String> solutions) {
        return solutions.stream()
            .map(DefaultSolution::new)
            .collect(toImmutableList());
    }

    @SuppressWarnings("unchecked")
    private static InternalAdditionalData toInternalAdditionalData(@Nullable AdditionalData<? extends AdditionalDataSpec> additionalData) {
        if (additionalData instanceof DeprecationData) {
            DeprecationData data = (DeprecationData) additionalData;
            return new DefaultDeprecationData(InternalDeprecationData.DeprecationType.valueOf(data.getType().name()));
        } else if (additionalData instanceof TypeValidationData) {
            TypeValidationData data = (TypeValidationData) additionalData;
            return new DefaultTypeValidationData(
                data.getPluginId(),
                data.isIrrelevantInErrorMessage(),
                data.getPropertyName(),
                data.getParentPropertyName(),
                data.getTypeName()
            );
        } else if (additionalData instanceof GenericData) {
            GenericData data = (GenericData) additionalData;
            return new DefaultAdditionalData(
                data.getAsMap().entrySet().stream()
                    .filter(entry -> isSupportedType(entry.getValue()))
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue))
            );
        } else {
            return new DefaultAdditionalData(Collections.emptyMap());
        }
    }

    private static boolean isSupportedType(Object type) {
        return type instanceof String;
    }
}
