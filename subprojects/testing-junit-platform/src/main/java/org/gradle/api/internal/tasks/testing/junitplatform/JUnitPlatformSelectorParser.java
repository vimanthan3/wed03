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

package org.gradle.api.internal.tasks.testing.junitplatform;

import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Will be replaced by JUnit Platform's {@link org.junit.platform.engine.discovery.DiscoverySelectors#parse(String)} once released
 */
public class JUnitPlatformSelectorParser {
    // TODO: Add ServiceLoader support for custom selector parsers
    public static List<DiscoverySelector> parse(List<String> selectorPatterns) {
        return JUnitPlatformSelectorParser.parseSelectors(selectorPatterns);
    }

    private static List<DiscoverySelector> parseSelectors(List<String> selectorPatterns) {
        return selectorPatterns.stream().flatMap(JUnitPlatformSelectorParser::parseSelector).collect(Collectors.toList());
    }

    private static Stream<DiscoverySelector> parseSelector(String selectorPattern) {
        // Preliminary selector pattern syntax based on URI syntax:
        // scheme - represents the type of selector
        // schemeSpecificParts - don't use `//` or `///` to make a URL and use the plain schemeSpecificParts instead
        // fragment - could be used for iteration selectors or method selectors
        URI uri = URI.create(selectorPattern);
        if (uri.getScheme() == null) {
            return Stream.empty();
        }
        switch (uri.getScheme()) {
            case "class":
                return Stream.of(DiscoverySelectors.selectClass(uri.getSchemeSpecificPart()));
            case "file":
                // TODO: will have to decide how we handle relative paths, e.g., file:///./path/to/file
                return Stream.of(DiscoverySelectors.selectFile(uri.getSchemeSpecificPart()));
            case "iteration":
                // TODO: Add support for iteration selectors when moving implementation to JUnit Platform
                // iteration:///urlEncodedBaseSelector#1,2,3
                return Stream.empty();
            case "method":
                return Stream.of(DiscoverySelectors.selectMethod(uri.getSchemeSpecificPart(), uri.getFragment()));
            case "module":
                return Stream.of(DiscoverySelectors.selectModule(uri.getSchemeSpecificPart()));
            case "package":
                return Stream.of(DiscoverySelectors.selectPackage(uri.getSchemeSpecificPart()));
            case "uniqueid":
                return Stream.of(DiscoverySelectors.selectUniqueId(uri.getSchemeSpecificPart()));
            default:
                return Stream.empty();
        }
    }
}
