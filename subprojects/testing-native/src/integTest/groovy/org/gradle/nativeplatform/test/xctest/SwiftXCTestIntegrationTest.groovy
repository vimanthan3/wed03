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

package org.gradle.nativeplatform.test.xctest

import org.gradle.nativeplatform.fixtures.AbstractInstalledToolChainIntegrationSpec
import org.gradle.nativeplatform.fixtures.NativeBinaryFixture
import org.gradle.nativeplatform.fixtures.RequiresInstalledToolChain
import org.gradle.nativeplatform.fixtures.ToolChainRequirement
import org.gradle.nativeplatform.fixtures.app.IncrementalSwiftXCTestAddDiscoveryBundle
import org.gradle.nativeplatform.fixtures.app.IncrementalSwiftXCTestRemoveDiscoveryBundle
import org.gradle.nativeplatform.fixtures.app.SwiftAppTest
import org.gradle.nativeplatform.fixtures.app.SwiftAppWithLibraries
import org.gradle.nativeplatform.fixtures.app.SwiftAppWithSingleXCTestSuite
import org.gradle.nativeplatform.fixtures.app.SwiftFailingXCTestBundle
import org.gradle.nativeplatform.fixtures.app.SwiftLib
import org.gradle.nativeplatform.fixtures.app.SwiftLibTest
import org.gradle.nativeplatform.fixtures.app.SwiftLibWithXCTest
import org.gradle.nativeplatform.fixtures.app.SwiftSingleFileApp
import org.gradle.nativeplatform.fixtures.app.SwiftSingleFileLibWithSingleXCTestSuite
import org.gradle.nativeplatform.fixtures.app.XCTestCaseElement
import org.gradle.nativeplatform.fixtures.app.XCTestSourceElement
import org.gradle.nativeplatform.fixtures.app.XCTestSourceFileElement
import spock.lang.Unroll

@RequiresInstalledToolChain(ToolChainRequirement.SWIFTC)
class SwiftXCTestIntegrationTest extends AbstractInstalledToolChainIntegrationSpec implements XCTestExecutionResult {
    def setup() {
        buildFile << """
apply plugin: 'xctest'
"""
    }

    def "fails when test cases fail"() {
        given:
        def testBundle = new SwiftFailingXCTestBundle()
        settingsFile << "rootProject.name = '${testBundle.projectName}'"
        buildFile << "apply plugin: 'swift-library'"
        testBundle.writeToProject(testDirectory)

        when:
        executer.withTasks("tasks").run()
        fails("test")

        then:
        result.assertTasksExecuted(":compileDebugSwift", ":compileTestSwift", ":linkTest", ":installTest", ":xcTest")
        testBundle.assertTestCasesRan(testExecutionResult)
    }

    def "succeeds when test cases pass"() {
        given:
        def lib = new SwiftLibWithXCTest()
        settingsFile << "rootProject.name = '${lib.projectName}'"
        buildFile << "apply plugin: 'swift-library'"
        lib.writeToProject(testDirectory)

        when:
        succeeds("test")

        then:
        result.assertTasksExecuted(":compileDebugSwift", ":compileTestSwift", ":linkTest", ":installTest", ":xcTest", ":test")
        lib.assertTestCasesRan(testExecutionResult)
    }

    def "does not execute removed test suite and case"() {
        given:
        def testBundle = new IncrementalSwiftXCTestRemoveDiscoveryBundle()
        assert testBundle.alternateFooTestSuite.testCount < testBundle.fooTestSuite.testCount

        settingsFile << "rootProject.name = 'app'"
        buildFile << "apply plugin: 'swift-library'"
        testBundle.writeToProject(testDirectory)

        when:
        succeeds("test")

        then:
        result.assertTasksExecuted(":compileDebugSwift", ":compileTestSwift", ":linkTest", ":installTest", ":xcTest", ":test")
        testBundle.assertTestCasesRan(testExecutionResult)

        when:
        testBundle.applyChangesToProject(testDirectory)
        succeeds("test")

        then:
        result.assertTasksExecuted(":compileDebugSwift", ":compileTestSwift", ":linkTest", ":installTest", ":xcTest", ":test")
        testBundle.assertAlternateTestCasesRan(testExecutionResult)
        testBundle.getFooTestSuite().getTestCount()
    }

    def "executes added test suite and case"() {
        given:
        def testBundle = new IncrementalSwiftXCTestAddDiscoveryBundle()
        assert testBundle.alternateFooTestSuite.testCount > testBundle.fooTestSuite.testCount

        settingsFile << "rootProject.name = 'app'"
        buildFile << "apply plugin: 'swift-library'"
        testBundle.writeToProject(testDirectory)

        when:
        succeeds("test")

        then:
        result.assertTasksExecuted(":compileDebugSwift", ":compileTestSwift", ":linkTest", ":installTest", ":xcTest", ":test")
        testBundle.assertTestCasesRan(testExecutionResult)

        when:
        testBundle.applyChangesToProject(testDirectory)
        succeeds("test")

        then:
        result.assertTasksExecuted(":compileDebugSwift", ":compileTestSwift", ":linkTest", ":installTest", ":xcTest", ":test")
        testBundle.assertAlternateTestCasesRan(testExecutionResult)
    }

    def "build logic can change source layout convention"() {
        given:
        def lib = new SwiftLibWithXCTest()
        settingsFile << "rootProject.name = '${lib.projectName}'"
        buildFile << "apply plugin: 'swift-library'"
        lib.main.writeToSourceDir(file("Sources"))
        lib.test.writeToSourceDir(file("Tests"))
        file("src/main/swift/broken.swift") << "ignore me!"
        file("src/test/swift/broken.swift") << "ignore me!"

        and:
        buildFile << """
            library {
                source.from 'Sources'
            }
            xctest {
                source.from 'Tests'
            }
         """

        expect:
        succeeds "test"
        result.assertTasksExecuted(":compileDebugSwift", ":compileTestSwift", ":linkTest", ":installTest", ":xcTest", ":test")

        file("build/obj/test").assertIsDir()
        executable("build/exe/test/${lib.test.moduleName}").assertExists()
        lib.assertTestCasesRan(testExecutionResult)
    }

    def "can specify a test dependency on another library"() {
        def lib = new SwiftLib()
        def test = new SwiftLibTest(lib, lib.greeter, lib.sum, lib.multiply)

        given:
        settingsFile << """
rootProject.name = 'app'
include 'greeter'
"""
        buildFile << """
project(':greeter') {
    apply plugin: 'swift-library'
}

apply plugin: 'swift-library'

dependencies {
    testImplementation project(':greeter')
}
"""
        lib.writeToProject(file('greeter'))
        test.writeToProject(testDirectory)

        when:
        succeeds("test")

        then:
        result.assertTasksExecuted(":greeter:compileDebugSwift", ":greeter:linkDebug", ":compileDebugSwift", ":compileTestSwift", ":linkTest", ":installTest", ":xcTest", ":test")
    }

    def "does not build or run any of the tests when assemble task executes"() {
        given:
        def testBundle = new SwiftFailingXCTestBundle()
        settingsFile << "rootProject.name = '${testBundle.projectName}'"
        testBundle.writeToProject(testDirectory)

        when:
        succeeds("assemble")

        then:
        result.assertTasksExecuted(":assemble")
        result.assertTasksSkipped(":assemble")
    }

    def "skips test tasks when no source is available for Swift library"() {
        given:
        buildFile << "apply plugin: 'swift-library'"

        when:
        succeeds("test")

        then:
        result.assertTasksExecuted(":compileDebugSwift", ":compileTestSwift", ":linkTest", ":installTest", ":xcTest", ":test")
        result.assertTasksSkipped(":compileDebugSwift", ":compileTestSwift", ":linkTest", ":installTest", ":xcTest", ":test")
    }

    def "skips test tasks when no source is available for Swift application"() {
        given:
        buildFile << """
apply plugin: 'swift-application'
"""

        when:
        succeeds("test")

        then:
        result.assertTasksExecuted(":compileDebugSwift", ":compileTestSwift", ":relocateMainForTest", ":linkTest", ":installTest", ":xcTest", ":test")
        result.assertTasksSkipped(":compileDebugSwift", ":compileTestSwift", ":relocateMainForTest", ":linkTest", ":installTest", ":xcTest", ":test")
    }

    def "can test public and internal features of a Swift application with a single source file"() {
        given:
        def main = new SwiftSingleFileApp()
        def test = new SwiftAppTest(main, main.greeter, main.sum, main.multiply)
        settingsFile << "rootProject.name = '${main.projectName}'"
        buildFile << """
apply plugin: 'swift-application'
"""
        main.writeToProject(testDirectory)
        test.writeToProject(testDirectory)

        when:
        succeeds("test")

        then:
        result.assertTasksExecuted(":compileDebugSwift", ":compileTestSwift", ":relocateMainForTest", ":linkTest", ":installTest", ":xcTest", ":test")
        assertMainSymbolIsAbsent(objectFiles(test, "build/obj/test"))
        test.assertTestCasesRan(testExecutionResult)
    }

    def "can test features of a Swift application using a single test source file"() {
        given:
        def app = new SwiftAppWithSingleXCTestSuite()
        settingsFile << "rootProject.name = '${app.projectName}'"
        buildFile << """
apply plugin: 'swift-application'
"""
        app.writeToProject(testDirectory)

        when:
        succeeds("test")

        then:
        result.assertTasksExecuted(":compileDebugSwift", ":compileTestSwift", ":relocateMainForTest", ":linkTest", ":installTest", ":xcTest", ":test")
        assertMainSymbolIsAbsent(objectFiles(app.test, "build/obj/test"))
        app.assertTestCasesRan(testExecutionResult)
    }

    def "can test features of a single file Swift library using a single test source file"() {
        given:
        def lib = new SwiftSingleFileLibWithSingleXCTestSuite()

        settingsFile << "rootProject.name = '${lib.projectName}'"
        buildFile << """
apply plugin: 'swift-library'
"""
        lib.writeToProject(testDirectory)

        when:
        succeeds("test")

        then:
        result.assertTasksExecuted(":compileDebugSwift", ":compileTestSwift", ":linkTest", ":installTest", ":xcTest", ":test")
        assertMainSymbolIsAbsent(objectFiles(lib.test, "build/obj/test"))
        assertMainSymbolIsAbsent(machOBundle("build/exe/test/${lib.test.moduleName}"))
        lib.assertTestCasesRan(testExecutionResult)
    }

    def "relinks when main sources change in ABI compatible way"() {
        given:
        def lib = new SwiftSingleFileLibWithSingleXCTestSuite()

        settingsFile << "rootProject.name = '${lib.projectName}'"
        buildFile << """
apply plugin: 'swift-library'
"""
        lib.writeToProject(testDirectory)

        when:
        succeeds("test")
        then:
        result.assertTasksExecuted(":compileDebugSwift", ":compileTestSwift", ":linkTest", ":installTest", ":xcTest", ":test")

        when:
        file("src/main/swift/combined.swift").replace("Hello,", "Goodbye,")
        then:
        succeeds("test")
        result.assertTasksExecuted(":compileDebugSwift", ":compileTestSwift", ":linkTest", ":installTest", ":xcTest", ":test")
        result.assertTasksSkipped(":compileTestSwift")
        result.assertTasksNotSkipped(":compileDebugSwift", ":linkTest", ":installTest", ":xcTest", ":test")
    }

    def "recompiles when main sources change in non-ABI compatible way"() {
        given:
        def lib = new SwiftSingleFileLibWithSingleXCTestSuite()

        settingsFile << "rootProject.name = '${lib.projectName}'"
        buildFile << """
apply plugin: 'swift-library'
"""
        lib.writeToProject(testDirectory)

        when:
        succeeds("test")
        then:
        result.assertTasksExecuted(":compileDebugSwift", ":compileTestSwift", ":linkTest", ":installTest", ":xcTest", ":test")

        when:
        file("src/main/swift/combined.swift").replace("sayHello", "sayAloha")
        then:
        fails("test")
        result.error.contains("value of type 'Greeter' has no member 'sayHello'")

        when:
        file("src/test/swift/CombinedTests.swift").replace("sayHello", "sayAloha")
        then:
        succeeds("test")
        result.assertTasksExecuted(":compileDebugSwift", ":compileTestSwift", ":linkTest", ":installTest", ":xcTest", ":test")
        result.assertTasksNotSkipped(":compileTestSwift", ":linkTest", ":installTest", ":xcTest", ":test")
    }

    def "build passes when tests have unicode characters"() {
        given:
        def test = new XCTestSourceElement("app") {
            List<XCTestSourceFileElement> testSuites = [
                new XCTestSourceFileElement("NormalTestSuite") {
                    List<XCTestCaseElement> testCases = [
                        passingTestCase("testExpectedTestName")]
                },

                new XCTestSourceFileElement("SpecialCharsTestSuite") {
                    List<XCTestCaseElement> testCases = [
                        passingTestCase("test_name_with_leading_underscore"),
                        passingTestCase("testname_with_a_number_1234"),
                        passingTestCase("test·middle_dot"),
                        passingTestCase("test1234"),
                        passingTestCase("testᏀᎡᎪᎠᏞᎬ")
                    ]
                },

                new XCTestSourceFileElement("UnicodeᏀᎡᎪᎠᏞᎬSuite") {
                    List<XCTestCaseElement> testCases = [
                        passingTestCase("testSomething"),
                    ]
                }
            ]
        }
        settingsFile << "rootProject.name = '${test.projectName}'"
        test.writeToProject(testDirectory)

        when:
        succeeds("test")

        then:
        result.assertTasksExecuted(":compileTestSwift", ":linkTest", ":installTest", ":xcTest", ":test")
        test.assertTestCasesRan(testExecutionResult)
    }

    def "build still fails when tests have unicode characters"() {
        given:
        def test = new XCTestSourceElement("app") {
            List<XCTestSourceFileElement> testSuites = [
                new XCTestSourceFileElement("NormalTestSuite") {
                    List<XCTestCaseElement> testCases = [
                        passingTestCase("testExpectedTestName")]
                },

                new XCTestSourceFileElement("SpecialCharsTestSuite") {
                    List<XCTestCaseElement> testCases = [
                        failingTestCase("test_name_with_leading_underscore"),
                        passingTestCase("testname_with_a_number_1234"),
                        failingTestCase("test·middle_dot"),
                        passingTestCase("test1234"),
                        passingTestCase("testᏀᎡᎪᎠᏞᎬ")
                    ]
                },

                new XCTestSourceFileElement("UnicodeᏀᎡᎪᎠᏞᎬSuite") {
                    List<XCTestCaseElement> testCases = [
                        failingTestCase("testSomething"),
                    ]
                }
            ]
        }
        settingsFile << "rootProject.name = '${test.projectName}'"
        test.writeToProject(testDirectory)

        when:
        fails("test")

        then:
        result.assertTasksExecuted(":compileTestSwift", ":linkTest", ":installTest", ":xcTest")
        test.assertTestCasesRan(testExecutionResult)
    }

    def 'can build xctest bundle which transitively dependencies on other Swift libraries'() {
        given:
        def app = new SwiftAppWithLibraries()
        settingsFile << """
            rootProject.name = 'app'
            include 'hello', 'log'
        """
        buildFile << """
            apply plugin: 'swift-application'
            dependencies {
                implementation project(':hello')
            }
            project(':hello') {
                apply plugin: 'swift-library'
                dependencies {
                    api project(':log')
                }
            }
            project(':log') {
                apply plugin: 'swift-library'
            }
        """

        app.application.writeToProject(testDirectory)
        app.greeter.writeToProject(file('hello'))
        app.logger.writeToProject(file('log'))

        file('src/test/swift/MainTest.swift') << """
            import XCTest
            import App
            
            public class MainTest : XCTestCase {
                public func testMain() {
                    XCTAssert(main() == 0)
                }
            } 
        """
        when:
        succeeds 'test'

        then:
        result.assertTasksExecuted(':log:compileDebugSwift', ':log:linkDebug',
            ':hello:compileDebugSwift', ':hello:linkDebug',
            ':compileDebugSwift', ':compileTestSwift', ":relocateMainForTest", ':linkTest', ':installTest', ':xcTest', ':test')
    }

    def 'can run xctest in swift package manager layout'() {
        given:
        def app = new SwiftAppWithLibraries()
        settingsFile << """
            rootProject.name = 'app'
            include 'hello', 'log'
        """
        buildFile << """
            apply plugin: 'swift-application'
            application {
                source.from rootProject.file('Sources/App')
            }
            xctest {
                source.from rootProject.file('Tests/AppTests')
            }
            dependencies {
                implementation project(':hello')
            }
            
            project(':hello') {
                apply plugin: 'swift-library'
                library {
                    source.from rootProject.file('Sources/Hello')
                }
                dependencies {
                    api project(':log')
                }
            }
            project(':log') {
                apply plugin: 'swift-library'
                library {
                    source.from rootProject.file('Sources/Log')
                }
            }
        """

        app.application.writeToProject(file('Sources/App'))
        app.greeter.writeToProject(file('Sources/Hello'))
        app.logger.writeToProject(file('Sources/Log'))

        file('Tests/AppTests/UtilTest.swift') << """
            import XCTest
            import App
            
            public class MainTest : XCTestCase {
                public func testMain() {
                    XCTAssert(main() == 0)
                }
            } 
        """
        when:
        succeeds 'test'

        then:
        result.assertTasksExecuted(':log:compileDebugSwift', ':log:linkDebug',
            ':hello:compileDebugSwift', ':hello:linkDebug',
            ':compileDebugSwift', ':compileTestSwift', ":relocateMainForTest", ':linkTest', ':installTest', ':xcTest', ':test')
    }

    @Unroll
    def "can use broken test filter [#testFilter]"() {
        given:
        def lib = new SwiftLibWithXCTest()
        settingsFile << "rootProject.name = '${lib.projectName}'"
        buildFile << "apply plugin: 'swift-library'"
        lib.writeToProject(testDirectory)

        when:
        runAndFail('xcTest', '--tests', testFilter)

        then:
        result.assertTasksExecuted(":compileDebugSwift", ":compileTestSwift", ":linkTest", ":installTest", ":xcTest")
        result.assertTasksNotSkipped(":compileDebugSwift", ":compileTestSwift", ":linkTest", ":installTest", ":xcTest")
        failure.assertHasCause("No tests found for given includes: [$testFilter](--tests filter)")

        where:
        testFilter << ['.SumTestSuite.testCanAddSumOf42', 'GreeterTest.SumTestSuite.', 'GreeterTest..testCanAddSumOf42']
    }

    private static void assertMainSymbolIsAbsent(List<NativeBinaryFixture> binaries) {
        binaries.each {
            assertMainSymbolIsAbsent(it)
        }
    }

    private static void assertMainSymbolIsAbsent(NativeBinaryFixture binary) {
        assert binary.binaryInfo.listSymbols().every { it.name != '_main' }
    }
}
