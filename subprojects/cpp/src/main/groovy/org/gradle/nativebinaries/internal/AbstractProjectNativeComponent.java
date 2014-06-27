/*
 * Copyright 2011 the original author or authors.
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
package org.gradle.nativebinaries.internal;

import org.gradle.api.DomainObjectSet;
import org.gradle.api.internal.DefaultDomainObjectSet;
import org.gradle.language.base.LanguageSourceSet;
import org.gradle.language.base.internal.LanguageSourceSetContainer;
import org.gradle.nativebinaries.NativeBinary;
import org.gradle.util.GUtil;

public abstract class AbstractProjectNativeComponent implements ProjectNativeComponentInternal {
    private final LanguageSourceSetContainer sourceSets = new LanguageSourceSetContainer();
    private final ProjectNativeComponentIdentifier id;
    private final DefaultDomainObjectSet<NativeBinary> binaries;

    private String baseName;

    public AbstractProjectNativeComponent(ProjectNativeComponentIdentifier id) {
        this.id = id;
        binaries = new DefaultDomainObjectSet<NativeBinary>(NativeBinary.class);
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    public String getProjectPath() {
        return id.getProjectPath();
    }

    public String getName() {
        return id.getName();
    }

    public DomainObjectSet<LanguageSourceSet> getSource() {
        return sourceSets;
    }

    public void source(Object sources) {
        sourceSets.source(sources);
    }

    public DomainObjectSet<NativeBinary> getBinaries() {
        return binaries;
    }

    public String getBaseName() {
        return GUtil.elvis(baseName, getName());
    }

    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }
}