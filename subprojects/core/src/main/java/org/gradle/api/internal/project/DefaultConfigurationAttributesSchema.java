/*
 * Copyright 2016 the original author or authors.
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
package org.gradle.api.internal.project;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.gradle.api.Attribute;
import org.gradle.api.AttributeMatchingStrategy;
import org.gradle.internal.Cast;

import java.util.List;
import java.util.Map;

public class DefaultConfigurationAttributesSchema implements org.gradle.api.AttributesSchema {
    private final Map<Attribute<?>, AttributeMatchingStrategy<?>> strategies = Maps.newHashMap();

    @Override
    public <T> void setMatchingStrategy(Attribute<T> attribute, AttributeMatchingStrategy<T> strategy) {
        strategies.put(attribute, strategy);
    }

    @Override
    public <T> AttributeMatchingStrategy<T> getMatchingStrategy(Attribute<T> attribute) {
        AttributeMatchingStrategy<?> strategy = strategies.get(attribute);
        if (strategy == null && String.class == attribute.getType()) {
            strategy = StringMatchingStrategy.INSTANCE;
        }
        if (strategy == null) {
            throw new IllegalArgumentException("Unable to find matching strategy for " + attribute);
        }
        return Cast.uncheckedCast(strategy);
    }

    private static class StringMatchingStrategy implements AttributeMatchingStrategy<String> {
        private static final StringMatchingStrategy INSTANCE = new StringMatchingStrategy();

        @Override
        public boolean isCompatible(String requestedValue, String candidateValue) {
            return requestedValue.equals(candidateValue);
        }

        @Override
        public <K> List<K> selectClosestMatch(String requestedValue, Map<K, String> compatibleValues) {
            return ImmutableList.copyOf(compatibleValues.keySet());
        }
    }
}
