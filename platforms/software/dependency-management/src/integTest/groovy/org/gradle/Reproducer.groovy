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

package org.gradle

import org.gradle.integtests.fixtures.AbstractIntegrationSpec

class Reproducer extends AbstractIntegrationSpec {
    def "test"() {
        def patch = mavenRepo.module("org", "transitive")
            .publish()

        mavenRepo.module("org", "direct")
            .dependsOn(patch)
            .publish()

        buildFile << """
            plugins {
                id("java-library")
            }

            configurations.testRuntimeClasspath.incoming.beforeResolve {
                dependencies.find { it instanceof ExternalModuleDependency && it.name.contains("direct") }
                            .exclude(group: "org", module: "transitive")
            }

            ${mavenTestRepository()}

            dependencyLocking {
                lockAllConfigurations()
            }

            dependencies {
                testImplementation(project)
                implementation 'org:direct:1.0'
            }

            task resolve {
                dependsOn(configurations.testRuntimeClasspath)
                def files = configurations.testRuntimeClasspath
                doLast {
                    println files*.name
                }
            }
        """

        expect:
        executer.noDeprecationChecks()
        succeeds("dependencies", "--write-locks")

        executer.noDeprecationChecks()
        succeeds("resolve")
    }

    def "test2"() {
        mavenRepo.module("org", "direct")
            .dependsOn(mavenRepo.module("org", "transitive").publish())
            .publish()

        buildFile << """
            plugins {
                id("java-library")
            }

            ${mavenTestRepository()}

            configurations.testRuntimeClasspath.incoming.beforeResolve {
                dependencies.find { it instanceof ExternalModuleDependency && it.name.contains("direct") }
                            .exclude(group: "org", module: "transitive")
            }

            dependencies {
                testImplementation(project)
                implementation 'org:direct:1.0'
            }

            task resolve {
                dependsOn(configurations.testRuntimeClasspath)
                def files = configurations.testRuntimeClasspath
                doLast {
                    assert(!files*.name.contains("transitive-1.0.jar"))
                }
            }
        """

        expect:
        executer.noDeprecationChecks()
        succeeds("resolve")
    }
}
