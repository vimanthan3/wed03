/*
 * Copyright 2024 the original author or authors.
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

package org.gradle.api.problems.internal;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class DefaultGenericData implements GenericData {

    private final Map<String, String> map;

    public DefaultGenericData(Map<String, String> map) {
        this.map = ImmutableMap.copyOf(map);
    }

    @Override
    public Map<String, String> getAsMap() {
        return map;
    }

    public static AdditionalDataBuilder<GenericData> builder() {
        return new DefaultGenericDataBuilder();
    }

    public static AdditionalDataBuilder<GenericData> builder(GenericData from) {
        return new DefaultGenericDataBuilder(from);
    }

    private static class DefaultGenericDataBuilder implements GenericDataSpec, AdditionalDataBuilder<GenericData> {
        private final ImmutableMap.Builder<String, String> mapBuilder = ImmutableMap.builder();

        private DefaultGenericDataBuilder() {
        }

        private DefaultGenericDataBuilder(GenericData from) {
            mapBuilder.putAll(from.getAsMap());
        }

        @Override
        public GenericDataSpec put(String key, String value) {
            mapBuilder.put(key, value);
            return this;
        }

        @Override
        public GenericData build() {
            return new DefaultGenericData(mapBuilder.build());
        }
    }
}
