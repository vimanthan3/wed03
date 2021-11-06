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

package org.gradle.buildinit.plugins.internal

import org.gradle.api.internal.DocumentationRegistry
import org.gradle.test.fixtures.file.TestFile

import static org.gradle.buildinit.plugins.internal.modifiers.BuildInitDsl.GROOVY

class BuildScriptBuilderGroovyTest extends AbstractBuildScriptBuilderTest {
    def builder = new BuildScriptBuilderFactory(new DocumentationRegistry()).scriptForNewProjects(GROOVY, "build", false)

    TestFile outputFile = tmpDir.file("build.gradle")

    def "generates basic groovy build script"() {
        when:
        builder.create(target).generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */
""")
    }

    def "generates basic groovy build script with @Incubating APIs warning"() {
        given:
        def builderUsingIncubating = new BuildScriptBuilderFactory(new DocumentationRegistry())
            .scriptForNewProjects(GROOVY, "build", true)

        when:
        builderUsingIncubating.create(target).generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 *
 * ${BuildScriptBuilder.incubatingApisWarning}
 */
""")
    }

    def "no spaces at the end of blank comment lines"() {
        when:
        builder.fileComment("\nnot-a-blank\n\nnot-a-blank")
        builder.create(target).generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 *
 * not-a-blank
 *
 * not-a-blank
 */
""")
    }

    def "can add groovy build script comment"() {
        when:
        builder.fileComment("""This is a sample
see more at gradle.org""")
        builder.create(target).generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 *
 * This is a sample
 * see more at gradle.org
 */
""")
    }

    def "can add plugins"() {
        when:
        builder.plugin("Add support for the Java language", "java")
        builder.plugin("Add support for Java libraries", "java-library")
        builder.plugin("Add support for the Kotlin language", "org.jetbrains.kotlin.jvm", "1.3.41")
        builder.create(target).generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    // Add support for the Java language
    id 'java'

    // Add support for Java libraries
    id 'java-library'

    // Add support for the Kotlin language
    id 'org.jetbrains.kotlin.jvm' version '1.3.41'
}
""")
    }

    def "can add repositories"() {
        given:
        builder.repositories().mavenLocal("Use maven local")
        builder.repositories().maven("Use another repo as well", "https://somewhere")
        builder.repositories().maven(null, "https://somewhere/2")
        builder.repositories().mavenCentral("Use Maven Central")

        when:
        builder.create(target).generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

repositories {
    // Use maven local
    mavenLocal()

    // Use another repo as well
    maven {
        url = uri('https://somewhere')
    }

    maven {
        url = uri('https://somewhere/2')
    }

    // Use Maven Central
    mavenCentral()
}
""")
    }

    def "can add compile dependencies"() {
        when:
        builder.implementationDependency("Use slf4j", "org.slf4j:slf4j-api:2.7", "org.slf4j:slf4j-simple:2.7")
        builder.implementationDependency(null, "a:b:1.2", "a:c:4.5")
        builder.implementationDependency(null, "a:d:4.5")
        builder.implementationDependency("Use Scala to compile", "org.scala-lang:scala-library:2.10")
        builder.dependencyWithExclusions("implementation", null, "a:e:1.2",
            new DependencyExclusion("a", "f"), new DependencyExclusion("a", "g"));
        builder.create(target).generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

dependencies {
    // Use slf4j
    implementation ('org.slf4j:slf4j-api:2.7')
    implementation ('org.slf4j:slf4j-simple:2.7')

    implementation ('a:b:1.2')
    implementation ('a:c:4.5')
    implementation ('a:d:4.5')

    // Use Scala to compile
    implementation ('org.scala-lang:scala-library:2.10')

    implementation ('a:e:1.2') {
        exclude (group: 'a', module: 'f')
        exclude (group: 'a', module: 'g')
    }
}
""")
    }

    def "can add test compile and runtime dependencies"() {
        when:
        builder.testImplementationDependency("use some test kit", "org:test:1.2", "org:test-utils:1.2")
        builder.testRuntimeOnlyDependency("needs some libraries at runtime", "org:test-runtime:1.2")
        builder.create(target).generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

dependencies {
    // use some test kit
    testImplementation ('org:test:1.2')
    testImplementation ('org:test-utils:1.2')

    // needs some libraries at runtime
    testRuntimeOnly ('org:test-runtime:1.2')
}
""")
    }

    def "can add platform dependencies"() {
        given:
        builder.dependencies().platformDependency("implementation", "use platform", "a:b:2.2")
        builder.dependencies().platformDependency("testImplementation", null, "a:c:2.0")
        builder.dependencies().platformDependency("implementation", null, "a:d:1.4")

        when:
        builder.create(target).generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

dependencies {
    // use platform
    implementation (platform('a:b:2.2'))

    implementation (platform('a:d:1.4'))
    testImplementation (platform('a:c:2.0'))
}
""")
    }

    def "can add project dependencies"() {
        given:
        builder.dependencies().projectDependency("implementation", "use some lib", ":abc")
        builder.dependencies().projectDependency("testImplementation", null, ":p1")
        builder.dependencies().projectDependency("implementation", null, ":p2")

        when:
        builder.create(target).generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

dependencies {
    // use some lib
    implementation (project(':abc'))

    implementation (project(':p2'))
    testImplementation (project(':p1'))
}
""")
    }

    def "can register tasks and refer to them later"() {
        given:
        def task = builder.taskRegistration("Compile stuff", "compile", "JavaCompile") { t ->
            t.propertyAssignment(null, "classpath", 12, true)
        }
        builder.block("Use stuff", "artifacts") { b ->
            b.propertyAssignment(null, "prop", task, true)
        }

        when:
        builder.create(target).generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

// Compile stuff
tasks.register('compile', JavaCompile) {
    classpath = 12
}

// Use stuff
artifacts {
    prop = tasks.compile
}
""")
    }

    def "can add property assignments"() {
        given:
        builder
            .propertyAssignment("Set a property", "foo.bar", "bazar")
            .propertyAssignment(null, "cathedral", 42)
            .propertyAssignment("Use a map", "cathedral", [a: 12, b: "value"])
            .propertyAssignment("Use a method call", "cathedral", builder.methodInvocationExpression("thing", 123, false))

        when:
        builder.create(target).generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

// Set a property
foo.bar = 'bazar'

cathedral = 42

// Use a map
cathedral = [a: 12, b: 'value']

// Use a method call
cathedral = thing(123, false)
""")
    }

    def "can add method invocations"() {
        given:
        builder
            .methodInvocation("No args", "foo.bar")
            .methodInvocation(null, "cathedral", 42)
            .methodInvocation("Use a map", "cathedral", [a: 12, b: "value"])
            .methodInvocation("Use a method call", "cathedral", builder.methodInvocationExpression("thing", [a: 12], 123, false))

        when:
        builder.create(target).generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

// No args
foo.bar()

cathedral(42)

// Use a map
cathedral(a: 12, b: 'value')

// Use a method call
cathedral(thing(a: 12, 123, false))
""")
    }

    def "can add code blocks"() {
        given:
        def block1 = builder.block("Add some thing", "foo.bar")
        block1.propertyAssignment(null, "foo.bar", "bazar", true)
        def block2 = builder.block("Do it again", "foo.bar")
        block2.propertyAssignment(null, "foo.bar", "bazar", true)
        builder.block(null, "other")

        when:
        builder.create(target).generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

// Add some thing
foo.bar {
    foo.bar = 'bazar'
}

// Do it again
foo.bar {
    foo.bar = 'bazar'
}

other {
}
""")
    }

    def "can add elements to top level containers and reference the element later"() {
        given:
        def e1 = builder.createContainerElement("Add some thing", "foo.bar", "e1", null)
        def e2 = builder.createContainerElement(null, "foo.bar", "e2", "someElement")
        builder.propertyAssignment(null, "prop", e1)
        builder.propertyAssignment(null, "prop2", builder.propertyExpression(e2, "outputDir"))

        when:
        builder.create(target).generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

// Add some thing
foo.bar {
    e1 {
    }
}

foo.bar {
    e2 {
    }
}

prop = foo.bar.e1
prop2 = foo.bar.e2.outputDir
""")
    }

    def "can add container elements to nested containers and reference the element later"() {
        given:
        builder.block("Add some thing", "foo") { b ->
            def element1 = b.containerElement("Element 1", "bar", "one", null) { e ->
                e.propertyAssignment(null, "value", "bazar", true)
                e.containerElement(null, "nested", "oneNested", null) {}
            }
            b.containerElement("Element 2", "bar", "two", null) { e ->
            }
            b.propertyAssignment("Use value", "prop", element1, true)
        }

        when:
        builder.create(target).generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

// Add some thing
foo {
    // Element 1
    bar {
        one {
            value = 'bazar'

            nested {
                oneNested {
                }
            }
        }
    }

    // Element 2
    bar {
        two {
        }
    }

    // Use value
    prop = bar.one
}
""")
    }

    def "can reference container elements in expressions"() {
        given:
        def e1 = builder.containerElementExpression("things", "element1")
        def e2 = builder.containerElementExpression("things.nested", "element2")
        def e3 = builder.propertyExpression(e2, "outputDir")
        def e4 = builder.methodInvocationExpression("extendsFrom", e3)
        builder.methodInvocation("Call method", e1, "thing", 12, e2)
        builder.propertyAssignment("Set property", "prop", e2)
        builder.propertyAssignment(null, "other", e4)

        when:
        builder.create(target).generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

// Call method
things.element1.thing(12, things.nested.element2)

// Set property
prop = things.nested.element2

other = extendsFrom(things.nested.element2.outputDir)
""")
    }

    def "can add further configuration"() {
        given:
        builder
            .taskPropertyAssignment(null, "test", "Test", "maxParallelForks", 23)
            .propertyAssignment("Set a property", "foo.bar", "bazar")
            .methodInvocation("Call a method", "foo.bar", "bazar", 12, builder.methodInvocationExpression("child", "a", 45))
            .block(null, "application") { b ->
                b.propertyAssignment("Define the main class for the application", "mainClass", "com.example.Main", false)
                b.propertyAssignment("Define the application name", "applicationName", "My Application", true)
            }
            .taskMethodInvocation("Use TestNG", "test", "Test", "useTestNG")
            .propertyAssignment(null, "cathedral", 42)
            .methodInvocation(null, "cathedral")
            .taskPropertyAssignment("Disable tests", "test", "Test", "enabled", false)
            .taskPropertyAssignment("Encoding", "Test", "encoding", "UTF-8")

        when:
        builder.create(target).generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

// Set a property
foo.bar = 'bazar'

// Call a method
foo.bar('bazar', 12, child('a', 45))

application {
    // Define the main class for the application
    mainClass = 'com.example.Main'

    // Define the application name
    applicationName = 'My Application'
}

cathedral = 42
cathedral()

tasks.withType(Test) {
    // Encoding
    encoding = 'UTF-8'
}

tasks.named('test') {
    maxParallelForks = 23

    // Use TestNG
    useTestNG()

    // Disable tests
    enabled = false
}
""")
    }

    def "statements can have multi-line comment"() {
        given:
        builder.repositories().mavenCentral("""
Use Maven Central

Alternatively:
- Could use Google's Maven repository
- Or a local mirror

""")

        when:
        builder.create(target).generate()

        then:
        assertOutputFile("/*\n" +
            " * This file was generated by the Gradle 'init' task.\n" +
            " */\n\n" +
            "repositories {\n" +
            "    // Use Maven Central\n" +
            "    // \n" + // mind the trailing whitespace!
            "    // Alternatively:\n" +
            "    // - Could use Google's Maven repository\n" +
            "    // - Or a local mirror\n" +
            "    mavenCentral()\n" +
            "}\n"
        )
    }

    def "vertical whitespace is included around statements with comments and and around blocks"() {
        given:
        builder.propertyAssignment(null, "foo", "bar")
        builder.methodInvocation(null, "foo", "bar")
        builder.propertyAssignment("has comment", "foo", "bar")
        builder.propertyAssignment(null, "foo", 123)
        builder.propertyAssignment(null, "foo", false)
        def b1 = builder.block(null, "block1")
        b1.methodInvocation("comment", "method1")
        b1.methodInvocation("comment", "method2")
        b1.methodInvocation(null, "method3")
        b1.methodInvocation(null, "method4")
        def b2 = builder.block("another block", "block2")
        b2.methodInvocation(null, "method1")
        b2.propertyAssignment(null, "foo", "bar", true)
        builder.propertyAssignment(null, "foo", "second last")
        builder.propertyAssignment(null, "foo", "last")

        when:
        builder.create(target).generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

foo = 'bar'
foo('bar')

// has comment
foo = 'bar'

foo = 123
foo = false

block1 {
    // comment
    method1()

    // comment
    method2()

    method3()
    method4()
}

// another block
block2 {
    method1()
    foo = 'bar'
}

foo = 'second last'
foo = 'last'
""")
    }

}
