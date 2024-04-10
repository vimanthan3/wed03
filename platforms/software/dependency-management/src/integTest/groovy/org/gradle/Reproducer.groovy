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
        def tools = mavenRepo.module("com.github.java-json-tools", "json-patch", "1.13")

        mavenRepo.module("org.jboss.resteasy", "resteasy-jackson2-provider", "6.2.4.Final")
            .parent("org.jboss.resteasy", "providers-pom", "6.2.4.Final")
            .dependsOn(exclusions: [[group: "com.google.code.findbugs", module: "jsr305"]], tools)
            .publish()

        buildFile << """
            plugins {
                id("java")
                id("io.spring.dependency-management") version '1.1.4'
            }

            configurations.all { conf ->
                conf.incoming.beforeResolve {
                    println("Resolving: " + name + " " + conf.excludeRules )
                    allDependencies.each {
                        println(it.toString() + " Excludes " + it.excludeRules.collect { it.group + ":" + it.module })
                    }
                }
            }

            ${mavenTestRepository()}
            ${mavenCentralRepository()}

            dependencyLocking {
                lockAllConfigurations()
            }

            dependencies {
                testImplementation(project)
                implementation 'org.jboss.resteasy:resteasy-jackson2-provider:6.2.4.Final'
            }

            task resolve {
                def files = configurations.testRuntimeClasspath
                doLast {
                    files.incoming.files.each { println it.name }
                }
            }
        """

        file("src/main/java/Foo.java") << "class Foo {}"
        file("src/test/java/Example.java") << "class Example {}"

        expect:
        executer.noDeprecationChecks()
        succeeds("dependencies", "--write-locks")

        println(file("gradle.lockfile").text)

        executer.noDeprecationChecks()
        succeeds("resolve")

        executer.noDeprecationChecks()
        succeeds("test", "-xcompileJava")

        // Why does spring depman not put excludes on the project dependency
        // after compileJava has run?
        executer.noDeprecationChecks()
        succeeds("test")
    }
}
