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
package org.gradle.api.internal.artifacts;

import org.gradle.api.artifacts.ConfigurationAttributes;

import java.util.Collections;
import java.util.Set;

public interface ConfigurationAttributesInternal extends ConfigurationAttributes {
    /**
     * An immutable empty configuration attributes map.
     */
    ConfigurationAttributesInternal EMPTY = new ConfigurationAttributesInternal() {
        @Override
        public Set<Key<?>> keySet() {
            return Collections.emptySet();
        }

        @Override
        public <T> ConfigurationAttributes attribute(Key<T> key, T value) {
            throw new UnsupportedOperationException("Mutation of attributes is not allowed");
        }

        @Override
        public <T> T getAttribute(Key<T> key) {
            return null;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean contains(Key<?> key) {
            return false;
        }

        @Override
        public ConfigurationAttributes asImmutable() {
            return this;
        }
    };

    /**
     * Returns an immutable view of this attribute set. Implementations are not required to return a distinct
     * instance for each call.
     * @return an immutable view of this container.
     */
    ConfigurationAttributes asImmutable();

}
