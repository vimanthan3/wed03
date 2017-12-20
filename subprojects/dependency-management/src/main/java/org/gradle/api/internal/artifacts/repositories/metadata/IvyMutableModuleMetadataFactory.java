/*
 * Copyright 2017 the original author or authors.
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
package org.gradle.api.internal.artifacts.repositories.metadata;

import com.google.common.collect.ImmutableList;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.internal.artifacts.ImmutableModuleIdentifierFactory;
import org.gradle.internal.component.external.model.DefaultMutableIvyModuleResolveMetadata;
import org.gradle.internal.component.external.model.IvyDependencyDescriptor;
import org.gradle.internal.component.external.model.MutableIvyModuleResolveMetadata;

public class IvyMutableModuleMetadataFactory implements MutableModuleMetadataFactory<MutableIvyModuleResolveMetadata> {
    private final ImmutableModuleIdentifierFactory moduleIdentifierFactory;

    public IvyMutableModuleMetadataFactory(ImmutableModuleIdentifierFactory moduleIdentifierFactory) {
        this.moduleIdentifierFactory = moduleIdentifierFactory;
    }

    @Override
    public MutableIvyModuleResolveMetadata create(ModuleComponentIdentifier from) {
        ModuleVersionIdentifier mvi = asVersionIdentifier(from);
        return new DefaultMutableIvyModuleResolveMetadata(mvi, from, ImmutableList.<IvyDependencyDescriptor>of());
    }

    protected ModuleVersionIdentifier asVersionIdentifier(ModuleComponentIdentifier from) {
        return moduleIdentifierFactory.moduleWithVersion(from.getGroup(), from.getModule(), from.getVersion());
    }

    @Override
    public MutableIvyModuleResolveMetadata missing(ModuleComponentIdentifier from) {
        MutableIvyModuleResolveMetadata metadata = create(from);
        metadata.setMissing(true);
        return metadata;
    }
}
