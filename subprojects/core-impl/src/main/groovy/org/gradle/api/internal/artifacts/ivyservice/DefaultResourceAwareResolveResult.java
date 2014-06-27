/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.api.internal.artifacts.ivyservice;

import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.ResourceAwareResolveResult;
import org.gradle.internal.resource.ExternalResourceName;

import java.util.ArrayList;
import java.util.List;

public class DefaultResourceAwareResolveResult implements ResourceAwareResolveResult {
    private final List<String> attempted = new ArrayList<String>();

    public List<String> getAttempted() {
        return attempted;
    }

    public void attempted(String locationDescription) {
        attempted.add(locationDescription);
    }

    public void attempted(ExternalResourceName location) {
        attempted(location.getDisplayName());
    }

    public void applyTo(ResourceAwareResolveResult target) {
        for (String location : attempted) {
            target.attempted(location);
        }
    }
}
